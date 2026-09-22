package com.ondo.photo;

import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.photo.domain.OwnerKind;
import com.ondo.photo.domain.ProfilePhoto;
import com.ondo.photo.domain.ProfilePhotoData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 프로필 이미지 저장/조회/삭제. 클라이언트가 1:1 크롭+리사이즈한 작은 이미지만 받는다(서버 이미지 처리 없음).
 * 소유권 검증은 호출부(PhotoController + ChildService)가 담당. 정본: 프로필 이미지 계획 Phase 1.
 *
 * 저장소는 두 테이블이다(V12) — 메타데이터(profile_photo)와 바이트(profile_photo_data).
 * 갱신시각만 필요한 경로는 메타데이터만 읽으므로 LONGBLOB 이 딸려올 자리가 없다.
 */
@Service
@Transactional(readOnly = true)
public class ProfilePhotoService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final int MAX_BYTES = 2 * 1024 * 1024; // 2MB — 크롭/리사이즈된 썸네일 전제

    private final ProfilePhotoRepository repository;
    private final ProfilePhotoDataRepository dataRepository;

    public ProfilePhotoService(ProfilePhotoRepository repository, ProfilePhotoDataRepository dataRepository) {
        this.repository = repository;
        this.dataRepository = dataRepository;
    }

    /** 이미지 응답용 — 바이트가 실제로 필요한 경로에서만 쓴다. */
    public record PhotoBytes(String contentType, byte[] data) {
    }

    /** 업서트 — 같은 소유자 사진이 있으면 교체. 반환: 갱신시각(캐시 키). */
    @Transactional
    public LocalDateTime save(OwnerKind ownerKind, Long ownerId, String contentType, byte[] data) {
        String type = normalizeType(contentType);
        if (!ALLOWED_TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "JPEG·PNG·WebP 이미지만 등록할 수 있어요.");
        }
        if (data == null || data.length == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "이미지가 비어 있어요.");
        }
        if (data.length > MAX_BYTES) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "이미지가 너무 커요. 더 작게 잘라 주세요.");
        }
        ProfilePhoto photo = repository.findByOwnerKindAndOwnerId(ownerKind, ownerId)
                .map(existing -> {
                    existing.replace(type);
                    return existing;
                })
                .orElseGet(() -> ProfilePhoto.of(ownerKind, ownerId, type));
        // 바이트 쪽 FK 가 이 행을 참조하므로 먼저 flush 해 둔다(새 사진일 때 순서가 어긋나면 FK 위반).
        LocalDateTime updatedAt = repository.saveAndFlush(photo).getUpdatedAt();

        ProfilePhotoData bytes = dataRepository.findByOwnerKindAndOwnerId(ownerKind, ownerId)
                .map(existing -> {
                    existing.replace(data);
                    return existing;
                })
                .orElseGet(() -> ProfilePhotoData.of(ownerKind, ownerId, data));
        dataRepository.save(bytes);
        return updatedAt;
    }

    /** 이미지 응답용 — 여기서만 LONGBLOB 을 읽는다. */
    public Optional<PhotoBytes> find(OwnerKind ownerKind, Long ownerId) {
        return repository.findByOwnerKindAndOwnerId(ownerKind, ownerId)
                .flatMap(meta -> dataRepository.findByOwnerKindAndOwnerId(ownerKind, ownerId)
                        .map(bytes -> new PhotoBytes(meta.getContentType(), bytes.getData())));
    }

    @Transactional
    public void delete(OwnerKind ownerKind, Long ownerId) {
        // FK 순서대로 바이트를 먼저 지운다. DB 쪽에도 ON DELETE CASCADE 가 걸려 있어 이중 안전장치다.
        dataRepository.deleteByOwnerKindAndOwnerId(ownerKind, ownerId);
        dataRepository.flush();
        repository.deleteByOwnerKindAndOwnerId(ownerKind, ownerId);
    }

    /** 단건 갱신시각(없으면 null) — 응답 photoUpdatedAt 채움용. 사진 바이트는 읽지 않는다. */
    public LocalDateTime updatedAtOrNull(OwnerKind ownerKind, Long ownerId) {
        return repository.findByOwnerKindAndOwnerId(ownerKind, ownerId)
                .map(ProfilePhoto::getUpdatedAt)
                .orElse(null);
    }

    /** 목록용 — ownerId → 갱신시각 맵(사진 없는 id 는 키 없음). 사진 바이트는 읽지 않는다. */
    public Map<Long, LocalDateTime> updatedAtByOwnerId(OwnerKind ownerKind, List<Long> ownerIds) {
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        return repository.findByOwnerKindAndOwnerIdIn(ownerKind, ownerIds).stream()
                .collect(Collectors.toMap(ProfilePhoto::getOwnerId, ProfilePhoto::getUpdatedAt));
    }

    private static String normalizeType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semi = contentType.indexOf(';');
        return (semi >= 0 ? contentType.substring(0, semi) : contentType).trim().toLowerCase();
    }
}
