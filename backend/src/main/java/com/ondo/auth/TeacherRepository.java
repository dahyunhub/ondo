package com.ondo.auth;

import com.ondo.auth.domain.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Teacher> findByProviderAndProviderId(String provider, String providerId);

    /**
     * 마지막 비밀번호 변경 시각만 조회(JwtAuthFilter 가 요청마다 부른다).
     * 엔티티를 로드하지 않도록 select 절을 컬럼 하나로 고정한다 — 인증은 모든 요청을 지나는 경로다.
     * 비밀번호를 바꾼 적 없거나 계정이 없으면 empty.
     */
    @Query("SELECT t.passwordChangedAt FROM Teacher t WHERE t.id = :id")
    Optional<LocalDateTime> findPasswordChangedAt(@Param("id") Long id);
}
