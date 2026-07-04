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
 * 교사 계정(FR-10, NFR-6). password_hash 는 BCrypt 해시만 저장(평문 금지).
 */
@Entity
@Table(name = "teacher")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Teacher extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column
    private String name;

    private Teacher(String email, String passwordHash, String name) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
    }

    /**
     * @param passwordHash 반드시 BCrypt 등으로 해시된 값(평문 금지)
     */
    public static Teacher create(String email, String passwordHash, String name) {
        return new Teacher(email, passwordHash, name);
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
}
