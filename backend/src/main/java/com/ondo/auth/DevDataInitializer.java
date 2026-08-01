package com.ondo.auth;

import com.ondo.auth.domain.Teacher;
import com.ondo.child.ChildRepository;
import com.ondo.child.domain.Child;
import com.ondo.child.domain.Gender;
import com.ondo.classroom.ClassroomRepository;
import com.ondo.classroom.domain.Classroom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * dev 프로파일 전용 시드 데이터. 운영(prod)·테스트에는 실행되지 않는다(시크릿 노출 방지, data-model-spec §5).
 * 로그인 데모용 교사 + 반 + 아이 명단을 한 번 주입해 로그인→반 선택→아이 CRUD 흐름을 바로 시연한다.
 * 로그인: teacher@ondo.dev / password1234
 */
@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private static final String DEV_EMAIL = "teacher@ondo.dev";
    private static final String DEV_RAW_PASSWORD = "password1234";

    private final TeacherRepository teacherRepository;
    private final ClassroomRepository classroomRepository;
    private final ChildRepository childRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(TeacherRepository teacherRepository,
                              ClassroomRepository classroomRepository,
                              ChildRepository childRepository,
                              PasswordEncoder passwordEncoder) {
        this.teacherRepository = teacherRepository;
        this.classroomRepository = classroomRepository;
        this.childRepository = childRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Teacher teacher = teacherRepository.findByEmail(DEV_EMAIL).orElse(null);
        if (teacher == null) {
            teacher = teacherRepository.save(
                    Teacher.create(DEV_EMAIL, passwordEncoder.encode(DEV_RAW_PASSWORD), "민지"));
            log.info("[dev] 시드 교사 생성: {} / {}", DEV_EMAIL, DEV_RAW_PASSWORD);
        }

        // 이미 반이 있으면 더 시드하지 않음(멱등)
        if (!classroomRepository.findClassroomSummaryRows(teacher.getId()).isEmpty()) {
            return;
        }

        // 만 4세반(age_class=4) → 출생연도 = year - (age_class + 1) = 2026 - 5 = 2021년생.
        Long classroomId = classroomRepository.save(
                Classroom.create(teacher.getId(), "만 4세반", 2026, 4, LocalDate.of(2026, 3, 2))).getId();

        record Seed(String name, LocalDate birth, Gender gender) {}
        List<Seed> kids = List.of(
                new Seed("강하준", LocalDate.of(2021, 3, 15), Gender.MALE),
                new Seed("김서준", LocalDate.of(2021, 1, 8), Gender.MALE),
                new Seed("김지호", LocalDate.of(2021, 11, 22), Gender.MALE),
                new Seed("박서윤", LocalDate.of(2021, 2, 27), Gender.FEMALE),
                new Seed("이도윤", LocalDate.of(2021, 5, 19), Gender.MALE),
                new Seed("정시우", LocalDate.of(2021, 9, 30), Gender.MALE),
                new Seed("최아인", LocalDate.of(2021, 4, 21), Gender.FEMALE),
                new Seed("한예준", LocalDate.of(2021, 7, 3), Gender.MALE),
                new Seed("오지안", LocalDate.of(2021, 12, 11), Gender.MALE),
                new Seed("윤하율", LocalDate.of(2021, 6, 14), Gender.FEMALE),
                new Seed("임서아", LocalDate.of(2021, 8, 25), Gender.FEMALE),
                new Seed("장은우", LocalDate.of(2021, 1, 30), Gender.MALE),
                new Seed("조유나", LocalDate.of(2021, 10, 7), Gender.FEMALE),
                new Seed("신도아", LocalDate.of(2021, 3, 2), Gender.FEMALE),
                new Seed("유주원", LocalDate.of(2021, 5, 28), Gender.MALE),
                new Seed("배소율", LocalDate.of(2021, 9, 16), Gender.FEMALE),
                new Seed("문지우", LocalDate.of(2021, 2, 9), Gender.FEMALE),
                new Seed("양건우", LocalDate.of(2021, 11, 5), Gender.MALE),
                new Seed("손채원", LocalDate.of(2021, 4, 13), Gender.FEMALE),
                new Seed("백시윤", LocalDate.of(2021, 7, 27), Gender.MALE),
                new Seed("홍서연", LocalDate.of(2021, 6, 1), Gender.FEMALE),
                new Seed("고은서", LocalDate.of(2021, 10, 19), Gender.FEMALE),
                new Seed("남주하", LocalDate.of(2021, 8, 8), Gender.FEMALE));

        char alias = 'A';
        for (Seed s : kids) {
            childRepository.save(Child.create(classroomId, s.name(), s.birth(), s.gender(), "아이" + alias));
            alias++;
        }
        log.info("[dev] 시드 반(만 4세반) + 아이 {}명 생성", kids.size());
    }
}
