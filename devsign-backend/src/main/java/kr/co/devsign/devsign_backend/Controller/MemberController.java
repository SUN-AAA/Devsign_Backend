package kr.co.devsign.devsign_backend.Controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.devsign.devsign_backend.Entity.Member;
import kr.co.devsign.devsign_backend.Service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/members")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        Member saved = memberService.signup(payload, request.getRemoteAddr());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/all")
    public List<Member> getAllMembers() {
        return memberService.getAllMembers();
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Member loginRequest, HttpServletRequest request) {
        return memberService.login(loginRequest, request);
    }

    @PostMapping("/logout-log")
    public Map<String, Object> logoutLog(@RequestBody Map<String, String> requestData, HttpServletRequest request) {
        return memberService.logoutLog(requestData, request.getRemoteAddr());
    }

    @PutMapping("/update/{loginId}")
    public Map<String, Object> updateMember(@PathVariable String loginId, @RequestBody Map<String, String> updateData) {
        return memberService.updateMember(loginId, updateData);
    }

    @PutMapping("/change-password/{loginId}")
    public Map<String, Object> changePassword(@PathVariable String loginId, @RequestBody Map<String, String> request) {
        return memberService.changePassword(loginId, request);
    }

    @PostMapping("/find-discord-by-info")
    public Map<String, Object> findDiscordByInfo(@RequestBody Map<String, String> request) {
        return memberService.findDiscordByInfo(request);
    }

    @PostMapping("/verify-id-pw")
    public Map<String, Object> verifyIdPw(@RequestBody Map<String, String> request) {
        return memberService.verifyIdPw(request);
    }

    @PutMapping("/reset-password-final")
    public Map<String, Object> resetPasswordFinal(@RequestBody Map<String, String> request) {
        return memberService.resetPasswordFinal(request);
    }

    @GetMapping("/check/{loginId}")
    public boolean checkId(@PathVariable String loginId) {
        return memberService.checkId(loginId);
    }

    @PostMapping("/discord-send")
    public Map<String, Object> sendDiscordCode(@RequestBody Map<String, String> request) {
        return memberService.sendDiscordCode(request);
    }

    @PostMapping("/verify-code")
    public Map<String, Object> verifyCode(@RequestBody Map<String, String> request) {
        return memberService.verifyCode(request);
    }
}
