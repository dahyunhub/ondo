package com.ondo.auth.domain;

import com.ondo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 교사 계정(FR-10, NFR-6).
 * <p>
 * 두 종류의 계정을 한 테이블로 표현한다.
 * <ul>
 *   <li>이메일/비번 계정: {@code email}·{@code passwordHash} 보유, {@code provider} NULL.</li>
 *   <li>소셜 계정(카카오): {@code provider}/{@code providerId} 보유, {@code passwordHash} NULL,
 *       카카오가 이메일을 주지 않으면 {@code email} 도 NULL.</li>
 * </ul>
 * password_hash 는 BCrypt 해시만 저장한다(평문 금지).
 */
@Entity
@Table(name = "teacher")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Teacher extends BaseTimeEntity {

    public static final String PROVIDER_KAKAO = "kakao";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이메일 미제공 소셜 계정은 NULL. UNIQUE(NULL 중복 허용). */
    @Column(unique = true)
    private String email;

    /** 소셜 전용 계정은 NULL. 비었으면 비밀번호 로그인 불가. */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column
    private String name;

    /** 소셜 로그인 제공자(예: {@code kakao}). 이메일/비번 계정은 NULL. */
    @Column(length = 20)
    private String provider;

    /** 제공자별 사용자 식별자(카카오 회원번호 문자열). (provider, provider_id) UNIQUE. */
    @Column(name = "provider_id", length = 64)
    private String providerId;

    private Teacher(String email, String passwordHash, String name, String provider, String providerId) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.provider = provider;
        this.providerId = providerId;
    }

    /**
     * 이메일/비밀번호 계정 생성.
     *
     * @param passwordHash 반드시 BCrypt 등으로 해시된 값(평문 금지)
     */
    public static Teacher create(String email, String passwordHash, String name) {
        return new Teacher(email, passwordHash, name, null, null);
    }

    /**
     * 카카오 신규 계정 생성. 비밀번호는 없고(NULL), 카카오가 이메일을 주지 않으면 email 은 NULL 이다.
     */
    public static Teacher createFromKakao(String providerId, String email, String name) {
        return new Teacher(email, null, name, PROVIDER_KAKAO, providerId);
    }

    public void changeName(String name) {
        this.name = name;
    }

    /**
     * @param passwordHash 반드시 BCrypt 등으로 해시된 값(평문 금지)
     */
    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** 소셜 로그인 사용 가능 여부(비밀번호 로그인만 가능한 계정이면 false). */
    public boolean hasPassword() {
        return passwordHash != null;
    }

    /** 기존 이메일 계정에 카카오를 연동한다(이후 두 방식 모두 로그인 가능). */
    public void linkKakao(String providerId) {
        this.provider = PROVIDER_KAKAO;
        this.providerId = providerId;
    }
}
