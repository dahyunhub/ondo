package com.ondo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 읽기 전용 데모 계정 설정(ondo.demo.*).
 * <p>
 * {@code enabled=true} 일 때만 데모 교사 계정과 시연용 데이터가 주입되고(멱등), 해당 계정의
 * 모든 변경 요청(POST/PUT/PATCH/DELETE)이 차단된다. 포트폴리오 라이브 데모에서 방문자가 자유롭게
 * 둘러보되 데이터는 훼손하지 못하게 하는 용도. 기본값은 비활성이라 테스트·일반 dev 실행엔 영향이 없다.
 */
@ConfigurationProperties(prefix = "ondo.demo")
public class DemoProperties {

    /** 데모 계정·시드 활성화 여부. 운영/로컬 데모에서만 true. */
    private boolean enabled = false;

    /** 데모 로그인 이메일(공개). */
    private String email = "demo@ondo.app";

    /** 데모 로그인 비밀번호(공개). 시드 시 BCrypt 로 해시해 저장한다. */
    private String password = "ondo-demo";

    /** 데모 교사 표시 이름(UI 가 "선생님"을 덧붙이므로 여기엔 붙이지 않는다). */
    private String name = "온도";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
