package com.ondo.auth;

import com.ondo.auth.domain.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Teacher> findByProviderAndProviderId(String provider, String providerId);
}
