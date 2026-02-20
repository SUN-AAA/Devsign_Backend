package kr.co.devsign.devsign_backend.Service;

import kr.co.devsign.devsign_backend.Entity.Member;
import kr.co.devsign.devsign_backend.Entity.Notice;
import kr.co.devsign.devsign_backend.Entity.NoticeView;
import kr.co.devsign.devsign_backend.Repository.MemberRepository;
import kr.co.devsign.devsign_backend.Repository.NoticeRepository;
import kr.co.devsign.devsign_backend.Repository.NoticeViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final MemberRepository memberRepository;
    private final NoticeViewRepository noticeViewRepository;
    private final AccessLogService accessLogService;

    public List<Notice> getAllNotices() {
        return noticeRepository.findAll(Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("id")));
    }

    public Map<String, Object> togglePin(Long id, String loginId, String ip) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));

        Map<String, Object> response = new HashMap<>();

        if (!notice.isPinned()) {
            long pinnedCount = noticeRepository.findAll().stream().filter(Notice::isPinned).count();
            if (pinnedCount >= 3) {
                response.put("status", "error");
                response.put("message", "고정 공지는 최대 3개까지만 가능합니다. ⚠️");
                return response;
            }
            notice.setPinned(true);
            accessLogService.logByLoginId(loginId, "NOTICE_PIN", ip);
        } else {
            notice.setPinned(false);
            accessLogService.logByLoginId(loginId, "NOTICE_UNPIN", ip);
        }

        noticeRepository.save(notice);
        response.put("status", "success");
        response.put("pinned", notice.isPinned());
        return response;
    }

    public Notice createNotice(Map<String, Object> payload, String loginId, String ip) {
        Notice notice = new Notice();
        notice.setTitle((String) payload.get("title"));
        notice.setContent((String) payload.get("content"));

        String category = (String) payload.get("category");
        notice.setCategory(category);
        notice.setTag(category);

        notice.setImages((List<String>) payload.get("images"));
        notice.setImportant(payload.get("important") != null && (Boolean) payload.get("important"));
        notice.setViews(0);
        notice.setDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")));
        notice.setPinned(false);

        if (loginId != null) {
            memberRepository.findByLoginId(loginId).ifPresent(m -> notice.setAuthor(m.getName()));
        } else {
            notice.setAuthor("관리자");
        }

        Notice saved = noticeRepository.save(notice);
        accessLogService.logByLoginId(loginId, "NOTICE_CREATE", ip);
        return saved;
    }

    public Notice updateNotice(Long id, Map<String, Object> payload, String loginId, String ip) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));

        notice.setTitle((String) payload.get("title"));
        notice.setContent((String) payload.get("content"));

        String category = (String) payload.get("category");
        notice.setCategory(category);
        notice.setTag(category);

        notice.setImages((List<String>) payload.get("images"));
        notice.setImportant(payload.get("important") != null && (Boolean) payload.get("important"));

        accessLogService.logByLoginId(loginId, "NOTICE_UPDATE", ip);
        return noticeRepository.save(notice);
    }

    public Notice getNoticeDetail(Long id, String loginId) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));

        if (loginId != null) {
            Optional<Member> memberOpt = memberRepository.findByLoginId(loginId);
            if (memberOpt.isPresent()) {
                Member member = memberOpt.get();

                if (!noticeViewRepository.existsByMemberAndNotice(member, notice)) {
                    notice.setViews(notice.getViews() + 1);
                    noticeRepository.save(notice);

                    NoticeView view = new NoticeView();
                    view.setMember(member);
                    view.setNotice(notice);
                    noticeViewRepository.save(view);
                }
            }
        }

        return notice;
    }

    @Transactional
    public Map<String, String> deleteNotice(Long id, String loginId, String ip) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));

        accessLogService.logByLoginId(loginId, "NOTICE_DELETE", ip);
        noticeViewRepository.deleteByNotice(notice);
        noticeRepository.delete(notice);

        return Map.of("status", "success");
    }
}
