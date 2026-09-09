package com.ondo.photo;

import com.ondo.photo.domain.OwnerKind;
import com.ondo.photo.domain.ProfilePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProfilePhotoRepository extends JpaRepository<ProfilePhoto, ProfilePhoto.PhotoId> {

    /** 사진 바이트가 실제로 필요한 경로(이미지 응답·업서트)용 — 엔티티 전체를 읽는다. */
    Optional<ProfilePhoto> findByOwnerKindAndOwnerId(OwnerKind ownerKind, Long ownerId);

    void deleteByOwnerKindAndOwnerId(OwnerKind ownerKind, Long ownerId);

    /**
     * 목록 화면용 — 여러 소유자의 사진 갱신시각 일괄 조회.
     *
     * 엔티티(ProfilePhoto)로 받으면 @Lob data(LONGBLOB)까지 같이 읽힌다. 기본 속성이라
     * 지연 로딩이 안 걸리기 때문이다. 아이 21명 반에서 명단 API 한 번이 응답 3KB 를 만들자고
     * MySQL 에서 5.6MB 를 끌어오고 있었다(bench: api.children.db_kb_per_req).
     * 그래서 select 절을 필요한 두 컬럼으로 못박는다.
     */
    @Query("SELECT p.ownerId AS ownerId, p.updatedAt AS updatedAt FROM ProfilePhoto p "
            + "WHERE p.ownerKind = :ownerKind AND p.ownerId IN :ownerIds")
    List<OwnerUpdatedAt> findUpdatedAtByOwnerIds(@Param("ownerKind") OwnerKind ownerKind,
                                                 @Param("ownerIds") List<Long> ownerIds);

    /** 단건 갱신시각 — 위와 같은 이유로 BLOB 을 읽지 않는다(아이 상세·교사 프로필 응답). */
    @Query("SELECT p.updatedAt FROM ProfilePhoto p "
            + "WHERE p.ownerKind = :ownerKind AND p.ownerId = :ownerId")
    Optional<LocalDateTime> findUpdatedAt(@Param("ownerKind") OwnerKind ownerKind,
                                          @Param("ownerId") Long ownerId);

    /** 갱신시각 조회 전용 projection — 사진 바이트가 들어올 자리가 없다. */
    interface OwnerUpdatedAt {
        Long getOwnerId();

        LocalDateTime getUpdatedAt();
    }
}
