package kr.co.devsign.devsign_backend.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String loginId;    // 로그인 아이디

    @Column(nullable = false)
    private String password;   // 비밀번호

    private String name;       // 이름
    private String studentId;  // 학번 (예: 20261234)

    private String dept;       // 소속 학과 (예: AI소프트웨어학부)
    private String interests; // 관심분야 (입력값)

    private String discordTag; // 디스코드 태그 (예: hm_kim)

    private String userStatus; // 재학생, 휴학생, LAB 등
    private String role;       // USER 또는 ADMIN

    // ✨ 계정 정지 여부 필드 추가
    // 기본값은 false(정상 상태)로 설정됩니다.
    private boolean suspended = false;

    @Column(columnDefinition = "LONGTEXT")
    private String profileImage;
}