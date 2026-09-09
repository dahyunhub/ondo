package com.ondo.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합테스트 베이스. MySQL 8 컨테이너를 싱글턴으로 한 번 띄우고 @ServiceConnection 으로 DataSource 를 주입한다.
 * Flyway V1 이 컨테이너에 적용된다(스키마 정본 검증).
 *
 * 싱글턴 패턴: static 블록에서 직접 start() 한다. @Testcontainers 로 관리하면 첫 테스트 클래스 종료 시
 * 컨테이너가 멈추는데, 캐시된 스프링 컨텍스트는 그 DataSource 를 계속 참조해 이후 클래스가 실패한다.
 * 컨테이너는 JVM 종료 시 Ryuk 가 정리한다.
 */
@SpringBootTest
public abstract class IntegrationTestSupport {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"));

    static {
        MYSQL.start();
    }

    @Autowired
    private JdbcTemplate integrationJdbcTemplate;

    @Autowired
    private PlatformTransactionManager integrationTxManager;

    /**
     * 인증 시도 카운터를 테스트마다 비운다.
     *
     * <p>{@code AuthThrottleService} 는 실패 횟수가 로그인 실패의 롤백에 휩쓸리지 않도록
     * {@code REQUIRES_NEW} 로 기록한다. 그래서 이 행들만은 <b>테스트 트랜잭션 밖에서 커밋되어
     * 롤백되지 않고</b> 다음 테스트로 새어 나간다. 일부러 로그인을 틀리는 테스트가 여럿이라,
     * 비우지 않으면 뒤에 실행된 테스트가 401 대신 429 를 받는다.
     * (Epic 4 회고의 "REQUIRES_NEW 커밋 데이터가 타 테스트에 보인다"와 같은 함정)
     *
     * <p><b>반드시 별도 트랜잭션에서 지운다.</b> 테스트 트랜잭션 안에서 지우면 그 트랜잭션이
     * auth_attempt 에 락을 쥔 채 테스트 본문으로 들어가고, 본문이 유발하는 REQUIRES_NEW 인서트가
     * 그 락을 기다리다 innodb_lock_wait_timeout(50초)에 걸려 500 이 된다. 실제로 그렇게 깨졌다.
     */
    @BeforeEach
    void clearAuthAttempts() {
        TransactionTemplate template = new TransactionTemplate(integrationTxManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.executeWithoutResult(status -> integrationJdbcTemplate.execute("DELETE FROM auth_attempt"));
    }
}
