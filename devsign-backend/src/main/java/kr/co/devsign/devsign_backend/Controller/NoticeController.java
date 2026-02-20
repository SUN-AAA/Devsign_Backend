package kr.co.devsign.devsign_backend.Controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.devsign.devsign_backend.Entity.Notice;
import kr.co.devsign.devsign_backend.Service.NoticeService;
import kr.co.devsign.devsign_backend.Util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notices")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<Notice> getAllNotices() {
        return noticeService.getAllNotices();
    }

    @PutMapping("/{id}/pin")
    public Map<String, Object> togglePin(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return noticeService.togglePin(id, loginId, request.getRemoteAddr());
    }

    @PostMapping
    public Notice createNotice(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return noticeService.createNotice(payload, loginId, request.getRemoteAddr());
    }

    @PutMapping("/{id}")
    public Notice updateNotice(@PathVariable Long id, @RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return noticeService.updateNotice(id, payload, loginId, request.getRemoteAddr());
    }

    @GetMapping("/{id}")
    public Notice getNoticeDetail(@PathVariable Long id, HttpServletRequest request) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return noticeService.getNoticeDetail(id, loginId);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteNotice(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return noticeService.deleteNotice(id, loginId, request.getRemoteAddr());
    }
}
