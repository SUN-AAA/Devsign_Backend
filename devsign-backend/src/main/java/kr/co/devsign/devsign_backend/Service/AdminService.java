package kr.co.devsign.devsign_backend.Service;

import kr.co.devsign.devsign_backend.Entity.Member;
import kr.co.devsign.devsign_backend.Repository.AccessLogRepository;
import kr.co.devsign.devsign_backend.Repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final MemberRepository memberRepository;
    private final AccessLogRepository accessLogRepository;
    private final AccessLogService accessLogService;
    private final DiscordBotClient discordBotClient;

    private static final Map<String, String> heroSettings = new ConcurrentHashMap<>();
    static {
        heroSettings.put("recruitmentText", "2026년 신입 부원 모집 중");
        heroSettings.put("applyLink", "https://open.kakao.com/o/example");
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAllByOrderByStudentIdDesc();
    }

    public List<?> getAllLogs() {
        return accessLogRepository.findAllByOrderByTimestampDesc();
    }

    public Map<String, String> getHeroSettings() {
        return heroSettings;
    }

    public Map<String, String> updateHeroSettings(Map<String, String> settings) {
        heroSettings.put("recruitmentText", settings.get("recruitmentText"));
        heroSettings.put("applyLink", settings.get("applyLink"));
        return Map.of("status", "success");
    }

    public Map<String, Object> syncDiscord() {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> botRes = discordBotClient.syncAllMembers();

            if (botRes != null && "success".equals(botRes.get("status"))) {
                List<Map<String, String>> discordMembers =
                        (List<Map<String, String>>) botRes.get("members");

                int updateCount = 0;

                for (Map<String, String> d : discordMembers) {
                    String tag = d.get("discordTag");

                    Optional<Member> opt = memberRepository.findByDiscordTag(tag);
                    if (opt.isPresent()) {
                        Member m = opt.get();
                        m.setName(d.get("name"));
                        m.setStudentId(d.get("studentId"));
                        m.setUserStatus(d.get("userStatus"));
                        m.setRole(d.get("role"));
                        m.setProfileImage(d.get("avatarUrl"));
                        memberRepository.save(m);
                        updateCount++;
                    }
                }

                response.put("status", "success");
                response.put("message", updateCount + "명의 부원 정보가 디스코드와 동기화되었습니다.");
                return response;
            }

            response.put("status", "fail");
            response.put("message", "봇 서버로부터 데이터를 받지 못했습니다.");
            return response;

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "동기화 중 서버 에러 발생: " + e.getMessage());
            return response;
        }
    }

    public Map<String, Object> toggleSuspension(Long id, String ip) {
        Map<String, Object> response = new HashMap<>();

        return memberRepository.findById(id)
                .map(m -> {
                    m.setSuspended(!m.isSuspended());
                    memberRepository.save(m);

                    accessLogService.logByMember(
                            m,
                            m.isSuspended() ? "ACCOUNT_SUSPEND" : "ACCOUNT_UNSUSPEND",
                            ip
                    );

                    response.put("status", "success");
                    return response;
                })
                .orElseGet(() -> {
                    response.put("status", "fail");
                    response.put("message", "사용자를 찾을 수 없습니다.");
                    return response;
                });
    }

    public Map<String, Object> restoreMember(Member member, String ip) {
        Map<String, Object> response = new HashMap<>();
        try {
            member.setId(null);
            Member saved = memberRepository.save(member);

            accessLogService.logByMember(saved, "ACCOUNT_RESTORE", ip);

            response.put("status", "success");
            return response;

        } catch (Exception e) {
            response.put("status", "fail");
            response.put("message", "복구 중 오류 발생: " + e.getMessage());
            return response;
        }
    }

    public Map<String, Object> deleteMember(Long id, boolean hard, String ip) {
        Map<String, Object> response = new HashMap<>();

        return memberRepository.findById(id)
                .map(m -> {
                    accessLogService.logByMember(
                            m,
                            hard ? "ACCOUNT_PERMANENT_DELETE" : "ACCOUNT_DELETE",
                            ip
                    );

                    memberRepository.deleteById(id);

                    response.put("status", "success");
                    return response;
                })
                .orElseGet(() -> {
                    response.put("status", "fail");
                    response.put("message", "삭제할 대상을 찾을 수 없습니다.");
                    return response;
                });
    }
}
