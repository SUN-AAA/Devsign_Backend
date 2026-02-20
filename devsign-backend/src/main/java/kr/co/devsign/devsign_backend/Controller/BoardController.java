package kr.co.devsign.devsign_backend.Controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.devsign.devsign_backend.Entity.Post;
import kr.co.devsign.devsign_backend.Service.BoardService;
import kr.co.devsign.devsign_backend.Util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<Post> getAllPosts() {
        return boardService.getAllPosts();
    }

    @PostMapping
    public Post createPost(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return boardService.createPost(payload, loginId, request.getRemoteAddr());
    }

    @GetMapping("/{id}")
    public Post getPostDetail(@PathVariable Long id, HttpServletRequest request) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return boardService.getPostDetail(id, loginId);
    }

    @PutMapping("/{id}")
    public Post updatePost(@PathVariable Long id, @RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return boardService.updatePost(id, payload, loginId, request.getRemoteAddr());
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deletePost(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return boardService.deletePost(id, loginId, request.getRemoteAddr());
    }

    @PostMapping("/{id}/like")
    public Post toggleLike(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return boardService.toggleLike(id, loginId, request.getRemoteAddr());
    }

    @PostMapping("/{id}/comments")
    public Post addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            HttpServletRequest request
    ) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return boardService.addComment(id, payload, loginId, request.getRemoteAddr());
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public Post deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            HttpServletRequest request
    ) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return boardService.deleteComment(postId, commentId, loginId, request.getRemoteAddr());
    }

    @PostMapping("/{postId}/comments/{commentId}/like")
    public Post toggleCommentLike(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            HttpServletRequest request
    ) {
        String loginId = jwtUtil.getLoginIdFromRequest(request);
        return boardService.toggleCommentLike(postId, commentId, loginId);
    }
}
