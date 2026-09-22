package com.ondo.photo;

import com.ondo.photo.domain.OwnerKind;
import com.ondo.photo.domain.ProfilePhoto;
import com.ondo.photo.domain.ProfilePhotoData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 사진 바이트 전용 리포지토리 — 여기의 조회는 전부 LONGBLOB 을 읽는다는 뜻이다.
 * 갱신시각·content_type 만 필요하면 {@link ProfilePhotoRepository} 를 쓴다.
 */
public interface ProfilePhotoDataRepository extends JpaRepository<ProfilePhotoData, ProfilePhoto.PhotoId> {

    Optional<ProfilePhotoData> findByOwnerKindAndOwnerId(OwnerKind ownerKind, Long ownerId);

    void deleteByOwnerKindAndOwnerId(OwnerKind ownerKind, Long ownerId);
}
