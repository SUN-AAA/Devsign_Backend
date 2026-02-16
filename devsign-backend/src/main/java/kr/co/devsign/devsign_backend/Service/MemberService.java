package kr.co.devsign.devsign_backend.Service;

import kr.co.devsign.devsign_backend.Entity.DiscordAuth;
import kr.co.devsign.devsign_backend.Entity.Member;
import kr.co.devsign.devsign_backend.Repository.DiscordAuthRepository;
import kr.co.devsign.devsign_backend.Repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final DiscordAuthRepository discordAuthRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private final AccessLogService accessLogService;
    private final DiscordBotClient discordBotClient;

    public Member signup(Map<String, String> payload, String ip) {
        String authCode = payload.get("authCode");

        DiscordAuth auth = discordAuthRepository.findByCode(authCode)
                .orElseThrow(() -> new RuntimeException("인증 코드가 올바르지 않거나 만료되었습니다."));

        Map<String, String> discordInfo = parseDiscordNickname(auth.getDiscordNickname());

        Member member = new Member();
        member.setLoginId(payload.get("loginId"));
        member.setPassword(passwordEncoder.encode(payload.get("password")));
        member.setDept(payload.get("dept"));
        member.setInterests(payload.get("interests"));

        member.setName(discordInfo.get("name"));
        member.setStudentId(discordInfo.get("studentId"));
        member.setDiscordTag(auth.getDiscordTag());

        member.setRole(auth.getRole() != null ? auth.getRole() : "USER");
        member.setUserStatus(auth.getUserStatus() != null ? auth.getUserStatus() : "ATTENDING");
        member.setProfileImage(auth.getAvatarUrl());

        Member saved = memberRepository.save(member);

        accessLogService.logByMember(saved, "SIGNUP", ip);
        discordAuthRepository.delete(auth);

        return saved;
    }

    public Map<String, Object> login(Member loginRequest, String ip) {
        Optional<Member> memberOpt = memberRepository.findByLoginId(loginRequest.getLoginId());
        Map<String, Object> response = new HashMap<>();

        if (memberOpt.isEmpty() || !passwordEncoder.matches(loginRequest.getPassword(), memberOpt.get().getPassword())) {
            response.put("status", "fail");
            response.put("message", "아이디 또는 비밀번호가 일치하지 않습니다.");
            return response;
        }

        Member m = memberOpt.get();

        if (m.isSuspended()) {
            response.put("status", "suspended");
            response.put("message", "정지된 아이디 입니다. 관리자에게 문의하세요.");
            return response;
        }

        accessLogService.logByMember(m, "LOGIN", ip);

        response.put("status", "success");
        response.put("role", m.getRole());
        response.put("userStatus", m.getUserStatus());
        response.put("name", m.getName());
        response.put("loginId", m.getLoginId());
        response.put("studentId", m.getStudentId());
        response.put("dept", m.getDept());
        response.put("discordTag", m.getDiscordTag());

        try {
            Map<String, Object> botRes = discordBotClient.getAvatar(m.getDiscordTag());
            if (botRes != null && "success".equals(botRes.get("status"))) {
                response.put("avatarUrl", botRes.get("avatarUrl"));
            } else {
                response.put("avatarUrl", "https://cdn.discordapp.com/embed/avatars/0.png");
            }
        } catch (Exception e) {
            response.put("avatarUrl", "https://cdn.discordapp.com/embed/avatars/0.png");
        }

        return response;
    }

    public Map<String, Object> logoutLog(Map<String, String> requestData, String ip) {
        accessLogService.logRaw(requestData.get("name"), requestData.get("studentId"), "LOGOUT", ip);
        return Map.of("status", "success");
    }

    public Map<String, Object> updateMember(String loginId, Map<String, String> updateData) {
        Optional<Member> memberOpt = memberRepository.findByLoginId(loginId);
        Map<String, Object> response = new HashMap<>();
        if (memberOpt.isEmpty()) {
            response.put("status", "fail");
            return response;
        }

        String newDiscordTag = updateData.get("discordTag");

        try {
            Map<String, Object> botRes = discordBotClient.checkMember(newDiscordTag);
            boolean exists = botRes != null && Boolean.TRUE.equals(botRes.get("exists"));
            if (!exists) {
                response.put("status", "fail");
                response.put("message", "입력하신 디스코드 태그를 동아리 서버에서 찾을 수 없습니다.");
                return response;
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "디스코드 연동 확인 중 서버 오류가 발생했습니다.");
            return response;
        }

        Member member = memberOpt.get();
        member.setDept(updateData.get("dept"));
        member.setDiscordTag(newDiscordTag);
        memberRepository.save(member);

        response.put("status", "success");
        return response;
    }

    public Map<String, Object> changePassword(String loginId, Map<String, String> request) {
        Optional<Member> memberOpt = memberRepository.findByLoginId(loginId);
        Map<String, Object> response = new HashMap<>();

        if (memberOpt.isEmpty()) {
            response.put("status", "fail");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return response;
        }

        Member member = memberOpt.get();
        String currentPw = request.get("currentPassword");
        String newPw = request.get("newPassword");

        if (!passwordEncoder.matches(currentPw, member.getPassword())) {
            response.put("status", "fail");
            response.put("message", "현재 비밀번호가 일치하지 않습니다.");
            return response;
        }

        member.setPassword(passwordEncoder.encode(newPw));
        memberRepository.save(member);

        response.put("status", "success");
        return response;
    }

    public Map<String, Object> findDiscordByInfo(Map<String, String> request) {
        String name = request.get("name");
        String studentId = request.get("studentId");

        return memberRepository.findByNameAndStudentId(name, studentId)
                .<Map<String, Object>>map(m -> {
                    Map<String, Object> res = new HashMap<>();
                    res.put("status", "success");
                    res.put("discordTag", m.getDiscordTag());
                    return res;
                })
                .orElseGet(() -> Map.of("status", "fail"));
    }

    public Map<String, Object> verifyIdPw(Map<String, String> request) {
        String name = request.get("name");
        String studentId = request.get("studentId");
        String inputCode = request.get("code");

        Optional<Member> memberOpt = memberRepository.findByNameAndStudentId(name, studentId);
        if (memberOpt.isEmpty()) return Map.of("status", "fail");

        String discordTag = memberOpt.get().getDiscordTag();
        boolean ok = checkVerificationInternal(discordTag, inputCode);

        if (!ok) return Map.of("status", "fail");

        Map<String, Object> res = new HashMap<>();
        res.put("status", "success");
        res.put("loginId", memberOpt.get().getLoginId());
        return res;
    }

    public Map<String, Object> resetPasswordFinal(Map<String, String> request) {
        String name = request.get("name");
        String studentId = request.get("studentId");
        String newPassword = request.get("newPassword");

        Optional<Member> memberOpt = memberRepository.findByNameAndStudentId(name, studentId);
        if (memberOpt.isEmpty()) return Map.of("status", "fail");

        Member member = memberOpt.get();
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);

        return Map.of("status", "success");
    }

    public boolean checkId(String loginId) {
        return memberRepository.findByLoginId(loginId).isPresent();
    }

    public Map<String, Object> sendDiscordCode(Map<String, String> request) {
        String discordTag = request.get("discordTag");
        String randomCode = String.format("%06d", (int) (Math.random() * 1000000));

        try {
            Map<String, Object> botRes = discordBotClient.sendCode(discordTag, randomCode);

            if (botRes != null && "success".equals(botRes.get("status"))) {
                DiscordAuth auth = new DiscordAuth();
                auth.setDiscordTag(discordTag);
                auth.setCode(randomCode);
                auth.setExpiry(LocalDateTime.now().plusMinutes(5));

                String nickname = botRes.get("studentId") + " " + botRes.get("name");
                auth.setDiscordNickname(nickname);
                auth.setRole((String) botRes.get("role"));
                auth.setUserStatus((String) botRes.get("userStatus"));
                auth.setAvatarUrl((String) botRes.get("avatarUrl"));

                discordAuthRepository.save(auth);
            }

            return botRes;

        } catch (Exception e) {
            return Map.of("status", "bot_error");
        }
    }

    public Map<String, Object> verifyCode(Map<String, String> request) {
        String discordTag = request.get("discordTag");
        String inputCode = request.get("code");

        Optional<DiscordAuth> authOpt =
                discordAuthRepository.findTopByDiscordTagOrderByExpiryDesc(discordTag.trim());

        if (authOpt.isPresent()) {
            DiscordAuth auth = authOpt.get();
            boolean ok = auth.getCode().equals(inputCode.trim())
                    && auth.getExpiry().isAfter(LocalDateTime.now());

            if (ok) {
                Map<String, String> discordInfo = parseDiscordNickname(auth.getDiscordNickname());

                Map<String, Object> res = new HashMap<>();
                res.put("status", "success");
                res.put("name", discordInfo.get("name"));
                res.put("studentId", discordInfo.get("studentId"));
                res.put("userStatus", auth.getUserStatus());
                res.put("role", auth.getRole());
                return res;
            }
        }

        return Map.of("status", "fail");
    }

    private boolean checkVerificationInternal(String discordTag, String code) {
        return discordAuthRepository.findTopByDiscordTagOrderByExpiryDesc(discordTag.trim())
                .map(auth -> auth.getCode().equals(code.trim()) && auth.getExpiry().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    private Map<String, String> parseDiscordNickname(String nickname) {
        Map<String, String> info = new HashMap<>();
        if (nickname != null && nickname.contains(" ")) {
            String[] parts = nickname.split(" ", 2);
            info.put("studentId", parts[0]);
            info.put("name", parts[1]);
        } else {
            info.put("studentId", "Unknown");
            info.put("name", nickname != null ? nickname : "Unknown");
        }
        return info;
    }
}

