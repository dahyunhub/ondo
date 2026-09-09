package com.ondo.photo;

import com.ondo.child.ChildService;
import com.ondo.photo.domain.OwnerKind;
import com.ondo.photo.dto.PhotoUploadedResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 프로필 이미지 API(아이·교사). 업로드는 크롭+리사이즈된 작은 이미지 바이트(image/jpeg|png|webp).
 * 소유권: 아이는 ChildService.assertOwnedChild, 교사는 본인(@AuthenticationPrincipal).
 */
@RestController
@RequestMapping("/api/v1")
public class PhotoController {

    /** 사진이 바뀌면 ETag(갱신시각)도 바뀌므로 길게 캐시해도 안전하다. */
    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofDays(365)).cachePrivate();

    private final ProfilePhotoService photoService;
    private final ChildService childService;

    public PhotoController(ProfilePhotoService photoService, ChildService childService) {
        this.photoService = photoService;
        this.childService = childService;
    }

    // ---------- 아이 ----------

    @PutMapping("/children/{childId}/photo")
    public PhotoUploadedResponse uploadChildPhoto(@AuthenticationPrincipal Long teacherId,
                                                  @PathVariable Long childId,
                                                  @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
                                                  @RequestBody byte[] data) {
        childService.assertOwnedChild(teacherId, childId);
        return new PhotoUploadedResponse(photoService.save(OwnerKind.CHILD, childId, contentType, data));
    }

    @GetMapping("/children/{childId}/photo")
    public ResponseEntity<byte[]> getChildPhoto(@AuthenticationPrincipal Long teacherId,
                                                @PathVariable Long childId,
                                                WebRequest request,
                                                HttpServletResponse response) {
        childService.assertOwnedChild(teacherId, childId);
        return photoResponse(OwnerKind.CHILD, childId, request, response);
    }

    @DeleteMapping("/children/{childId}/photo")
    public ResponseEntity<Void> deleteChildPhoto(@AuthenticationPrincipal Long teacherId,
                                                 @PathVariable Long childId) {
        childService.assertOwnedChild(teacherId, childId);
        photoService.delete(OwnerKind.CHILD, childId);
        return ResponseEntity.noContent().build();
    }

    // ---------- 교사(본인) ----------

    @PutMapping("/teachers/me/photo")
    public PhotoUploadedResponse uploadMyPhoto(@AuthenticationPrincipal Long teacherId,
                                               @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
                                               @RequestBody byte[] data) {
        return new PhotoUploadedResponse(photoService.save(OwnerKind.TEACHER, teacherId, contentType, data));
    }

    @GetMapping("/teachers/me/photo")
    public ResponseEntity<byte[]> getMyPhoto(@AuthenticationPrincipal Long teacherId,
                                             WebRequest request,
                                             HttpServletResponse response) {
        return photoResponse(OwnerKind.TEACHER, teacherId, request, response);
    }

    @DeleteMapping("/teachers/me/photo")
    public ResponseEntity<Void> deleteMyPhoto(@AuthenticationPrincipal Long teacherId) {
        photoService.delete(OwnerKind.TEACHER, teacherId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 사진 응답 — 재검증(If-None-Match) 요청이면 사진 바이트를 읽지 않는다.
     *
     * 예전에는 엔티티를 통째로 읽은 뒤 ResponseEntity 의 ETag 로 304 를 만들었다. 네트워크는
     * 아꼈지만 DB 는 매번 LONGBLOB 을 읽었다(bench: etag.revalidate_db_kb 325KB). 아바타가
     * 아이 수만큼 붙는 명단 화면에서는 재방문 한 번이 사진 전량을 헛읽는 셈이었다.
     * 그래서 ETag 재료인 갱신시각만 먼저 읽고, 실제로 바뀐 경우에만 바이트를 가져온다.
     */
    private ResponseEntity<byte[]> photoResponse(OwnerKind ownerKind, Long ownerId,
                                                 WebRequest request, HttpServletResponse response) {
        LocalDateTime updatedAt = photoService.updatedAtOrNull(ownerKind, ownerId);
        if (updatedAt == null) {
            return ResponseEntity.notFound().build();
        }
        String etag = "\"" + updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli() + "\"";
        if (request.checkNotModified(etag)) {
            // checkNotModified 가 304 와 ETag 를 세팅한다(약한 검증자·다중 값 파싱 포함).
            // Cache-Control 은 직접 실어 200 응답과 같은 캐시 정책을 재검증 응답에서도 유지한다.
            // null 반환 = "응답을 이미 다 만들었다"는 Spring MVC 규약(HttpEntityMethodProcessor).
            response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE.getHeaderValue());
            return null;
        }
        // 두 조회 사이에 사진이 지워졌을 수 있으므로 없으면 404.
        return photoService.find(ownerKind, ownerId)
                .map(p -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(p.getContentType()))
                        .cacheControl(CACHE)
                        .eTag(etag)
                        .body(p.getData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
