package kr.co.devsign.devsign_backend.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;  // "학술", "친목", "대회" [cite: 121]
    private String title;     // 행사 제목 [cite: 122]
    private String date;      // 행사 일시 [cite: 123]
    private String location;  // 장소 [cite: 123]

    @Column(columnDefinition = "TEXT")
    private String content;   // 상세 내용
    private String image;     // 이미지 URL [cite: 120]

    private int views = 0;    // 조회수 [cite: 124] 초기값 = 0
    private int likes = 0;    // 좋아요 수 초기값 = 0
}