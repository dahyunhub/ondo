package com.ondo.auth.throttle;

/**
 * 시도 카운터를 나누는 축. 경로마다 임계치가 달라야 해서 스코프를 분리한다
 * (예: 비밀번호 확인은 이미 유효 토큰을 쥔 공격자가 두드리는 곳이라 로그인보다 빡빡하다).
 */
public enum AuthAttemptScope {
    /** 로그인 — 키: 정규화된 이메일. */
    LOGIN,
    /** 현재 비밀번호 확인(프로필의 비밀번호 변경) — 키: teacherId. */
    PASSWORD_CONFIRM,
    /** 비밀번호 재설정 메일 요청 — 키: 정규화된 이메일. */
    RESET_REQUEST
}
