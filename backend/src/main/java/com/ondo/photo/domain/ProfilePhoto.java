package com.ondo.photo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * 프로필 이미지의 메타데이터(아이·교사). 테이블 profile_photo 는 Flyway V3 정본, V12 에서 BLOB 분리.
 * 복합 PK(owner_kind, owner_id).
 *
 * 이미지 바이트는 여기에 없다 — {@link ProfilePhotoData} 로 분리돼 있다. 같은 행에 두면 갱신시각
 * 하나를 읽는 조회에도 이미지가 딸려오고, 실제로 그렇게 터진 적이 있다(V12 주석 참고).
 * 이 엔티티를 아무리 통째로 읽어도 이미지는 따라오지 않는다.
 */
@Entity
@Table(name = "profile_photo")
@IdClass(ProfilePhoto.PhotoId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfilePhoto {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_kind", nullable = false, length = 10)
    private OwnerKind ownerKind;

    @Id
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ProfilePhoto(OwnerKind ownerKind, Long ownerId, String contentType) {
        this.ownerKind = ownerKind;
        this.ownerId = ownerId;
        replace(contentType);
    }

    public static ProfilePhoto of(OwnerKind ownerKind, Long ownerId, String contentType) {
        return new ProfilePhoto(ownerKind, ownerId, contentType);
    }

    /** 같은 소유자의 사진 교체(업서트 시 기존 행 갱신). 갱신시각은 ETag·캐시 키로 쓰인다. */
    public void replace(String contentType) {
        this.contentType = contentType;
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    /** 복합 PK 클래스. */
    public static class PhotoId implements Serializable {
        private OwnerKind ownerKind;
        private Long ownerId;

        public PhotoId() {
        }

        public PhotoId(OwnerKind ownerKind, Long ownerId) {
            this.ownerKind = ownerKind;
            this.ownerId = ownerId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PhotoId that)) return false;
            return ownerKind == that.ownerKind && Objects.equals(ownerId, that.ownerId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ownerKind, ownerId);
        }
    }
}
