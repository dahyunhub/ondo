package com.ondo.photo;

import com.ondo.photo.domain.OwnerKind;
import com.ondo.photo.domain.ProfilePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 사진 메타데이터(content_type·updated_at) 리포지토리 — 이미지 바이트는 다루지 않는다.
 *
 * V12 이전에는 이 테이블에 LONGBLOB 이 같이 있어서, 갱신시각만 필요한 자리에서도 엔티티를 반환하면
 * 사진 원본이 딸려왔다(아이 명단 API 가 응답 0.6KB 에 5.4MB 를 읽던 원인). 그때는 select 절을
 * 못박는 projection 으로 막았지만, 지금은 테이블이 분리돼 엔티티를 통째로 읽어도 안전하다 —
 * 그래서 projection 을 걷어내고 파생 쿼리로 되돌렸다. 바이트가 필요하면
 * {@link ProfilePhotoDataRepository} 를 쓴다.
 */
public interface ProfilePhotoRepository extends JpaRepository<ProfilePhoto, ProfilePhoto.PhotoId> {

    Optional<ProfilePhoto> findByOwnerKindAndOwnerId(OwnerKind ownerKind, Long ownerId);

    void deleteByOwnerKindAndOwnerId(OwnerKind ownerKind, Long ownerId);

    /** 목록 화면용 — 여러 소유자의 사진 갱신시각 일괄 조회. */
    List<ProfilePhoto> findByOwnerKindAndOwnerIdIn(OwnerKind ownerKind, List<Long> ownerIds);
}
