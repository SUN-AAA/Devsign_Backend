package kr.co.devsign.devsign_backend.Service;

import kr.co.devsign.devsign_backend.Entity.*;
import kr.co.devsign.devsign_backend.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final EventViewRepository eventViewRepository;
    private final EventLikeRepository eventLikeRepository;

    private final AccessLogService accessLogService;

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event createEvent(Map<String, Object> payload, String loginId, String ip) {
        Event event = new Event();
        event.setCategory((String) payload.get("category"));
        event.setTitle((String) payload.get("title"));
        event.setDate((String) payload.get("date"));
        event.setLocation((String) payload.get("location"));
        event.setContent((String) payload.get("content"));
        event.setImage((String) payload.get("image"));
        event.setViews(0);
        event.setLikes(0);

        Event saved = eventRepository.save(event);
        accessLogService.logByLoginId(loginId, "EVENT_CREATE", ip);
        return saved;
    }

    public Event updateEvent(Long id, Map<String, Object> payload, String loginId, String ip) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("행사를 찾을 수 없습니다."));

        event.setCategory((String) payload.get("category"));
        event.setTitle((String) payload.get("title"));
        event.setDate((String) payload.get("date"));
        event.setLocation((String) payload.get("location"));
        event.setContent((String) payload.get("content"));
        event.setImage((String) payload.get("image"));

        accessLogService.logByLoginId(loginId, "EVENT_UPDATE", ip);
        return eventRepository.save(event);
    }

    public Map<String, Object> getEventDetail(Long id, String loginId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("행사를 찾을 수 없습니다."));

        boolean isLiked = false;

        if (loginId != null && !loginId.isBlank()) {
            Optional<Member> memberOpt = memberRepository.findByLoginId(loginId);
            if (memberOpt.isPresent()) {
                Member member = memberOpt.get();

                if (!eventViewRepository.existsByMemberAndEvent(member, event)) {
                    event.setViews(event.getViews() + 1);
                    eventRepository.save(event);

                    EventView view = new EventView();
                    view.setMember(member);
                    view.setEvent(event);
                    eventViewRepository.save(view);
                }

                isLiked = eventLikeRepository.existsByMemberAndEvent(member, event);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("event", event);
        response.put("isLiked", isLiked);
        return response;
    }

    @Transactional
    public Map<String, Object> toggleLike(Long id, String loginId) {
        Map<String, Object> response = new HashMap<>();

        if (loginId == null || loginId.isBlank()) {
            response.put("status", "error");
            response.put("message", "로그인이 필요합니다.");
            return response;
        }

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("행사를 찾을 수 없습니다."));
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (eventLikeRepository.existsByMemberAndEvent(member, event)) {
            eventLikeRepository.deleteByMemberAndEvent(member, event);
            event.setLikes(Math.max(0, event.getLikes() - 1));
            response.put("liked", false);
        } else {
            EventLike like = new EventLike();
            like.setMember(member);
            like.setEvent(event);
            eventLikeRepository.save(like);

            event.setLikes(event.getLikes() + 1);
            response.put("liked", true);
        }

        eventRepository.save(event);

        response.put("likeCount", event.getLikes());
        response.put("status", "success");
        return response;
    }

    @Transactional
    public Map<String, String> deleteEvent(Long id, String loginId, String ip) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("행사를 찾을 수 없습니다."));

        accessLogService.logByLoginId(loginId, "EVENT_DELETE", ip);

        eventLikeRepository.deleteByEvent(event);
        eventViewRepository.deleteByEvent(event);

        eventRepository.delete(event);

        return Map.of("status", "success");
    }
}
