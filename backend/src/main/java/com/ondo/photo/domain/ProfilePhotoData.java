package com.ondo.photo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 이미지의 바이트만 담는 테이블(Flyway V12). PK 는 {@link ProfilePhoto} 와 같고 FK 로 묶여 있다
 * (ON DELETE CASCADE).
 *
 * 왜 테이블을 나눴나: 메타데이터와 같은 행에 있으면 갱신시각 하나를 읽는 조회에도 LONGBLOB 이
 * 딸려온다. @Lob byte[] 는 기본 속성이라 지연 로딩이 걸리지 않기 때문이다. 조회 코드를 조심해서
 * 쓰는 걸로 막으려면 새 조회가 생길 때마다 같은 주의가 필요하지만, 테이블을 나누면 그럴 자리가 없다.
 *
 * 그러므로 이 엔티티는 사진 바이트가 실제로 필요할 때만 조회한다(이미지 응답·업서트).
 */
@Entity
@Table(name = "profile_photo_data")
@IdClass(ProfilePhoto.PhotoId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfilePhotoData {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_kind", nullable = false, length = 10)
    private OwnerKind ownerKind;

    @Id
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;

    private ProfilePhotoData(OwnerKind ownerKind, Long ownerId, byte[] data) {
        this.ownerKind = ownerKind;
        this.ownerId = ownerId;
        this.data = data;
    }

    public static ProfilePhotoData of(OwnerKind ownerKind, Long ownerId, byte[] data) {
        return new ProfilePhotoData(ownerKind, ownerId, data);
    }

    /** 같은 소유자의 사진 바이트 교체(업서트 시 기존 행 갱신). */
    public void replace(byte[] data) {
        this.data = data;
    }
}
