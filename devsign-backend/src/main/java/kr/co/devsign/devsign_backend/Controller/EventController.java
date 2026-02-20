package kr.co.devsign.devsign_backend.Controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.devsign.devsign_backend.Entity.Event;
import kr.co.devsign.devsign_backend.Service.EventService;
import kr.co.devsign.devsign_backend.Util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<Event> getAllEvents() {
        return eventService.getAllEvents();
    }

    @PostMapping
    public Event createEvent(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return eventService.createEvent(payload, loginId, request.getRemoteAddr());
    }

    @PutMapping("/{id}")
    public Event updateEvent(@PathVariable Long id, @RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return eventService.updateEvent(id, payload, loginId, request.getRemoteAddr());
    }

    @GetMapping("/{id}")
    public Map<String, Object> getEventDetail(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        // 비로그인 사용자도 조회 가능 (null 허용)
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return eventService.getEventDetail(id, loginId);
    }

    @PostMapping("/{id}/like")
    public Map<String, Object> toggleLike(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return eventService.toggleLike(id, loginId);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteEvent(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return eventService.deleteEvent(id, loginId, request.getRemoteAddr());
    }
}
