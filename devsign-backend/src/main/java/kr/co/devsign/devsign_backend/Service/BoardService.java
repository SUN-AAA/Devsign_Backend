package kr.co.devsign.devsign_backend.Service;

import kr.co.devsign.devsign_backend.Entity.*;
import kr.co.devsign.devsign_backend.Repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostViewRepository postViewRepository;
    private final CommentLikeRepository commentLikeRepository;

    private final AccessLogService accessLogService;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Post> getAllPosts() {
        return postRepository.findAllByOrderByIdDesc();
    }

    public Post createPost(Map<String, Object> payload, String ip) {
        String loginId = (String) payload.get("loginId");
        Member member = memberRepository.findByLoginId(loginId).orElseThrow();

        Post post = new Post();
        post.setTitle((String) payload.get("title"));
        post.setContent((String) payload.get("content"));
        post.setCategory((String) payload.get("category"));

        Object imagesObj = payload.get("images");
        if (imagesObj instanceof List) {
            post.setImages((List<String>) imagesObj);
        }

        post.setAuthor(member.getName());
        post.setLoginId(member.getLoginId());
        post.setStudentId(member.getStudentId());
        post.setProfileImage(member.getProfileImage());
        post.setDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM.dd HH:mm")));

        Post saved = postRepository.save(post);
        accessLogService.logByLoginId(loginId, "POST_CREATE", ip);
        return saved;
    }

    @Transactional
    public Post getPostDetail(Long id, String loginId) {
        Post post = postRepository.findById(id).orElseThrow();
        Member member = memberRepository.findByLoginId(loginId).orElse(null);

        if (member != null) {
            if (!postViewRepository.existsByMemberAndPost(member, post)) {
                post.setViews(post.getViews() + 1);
                postRepository.save(post);

                PostView view = new PostView();
                view.setMember(member);
                view.setPost(post);
                postViewRepository.save(view);
            }
            syncLikedStatus(post, member);
        }
        return post;
    }

    public Post updatePost(Long id, Map<String, Object> payload, String ip) {
        Post post = postRepository.findById(id).orElseThrow();
        post.setTitle((String) payload.get("title"));
        post.setContent((String) payload.get("content"));
        post.setCategory((String) payload.get("category"));

        Object imagesObj = payload.get("images");
        if (imagesObj instanceof List) {
            post.setImages((List<String>) imagesObj);
        }

        accessLogService.logByLoginId((String) payload.get("loginId"), "POST_UPDATE", ip);
        return postRepository.save(post);
    }

    @Transactional
    public Map<String, String> deletePost(Long id, String loginId, String ip) {
        Post post = postRepository.findById(id).orElseThrow();

        postLikeRepository.deleteByPost(post);
        postViewRepository.deleteByPost(post);

        for (Comment comment : post.getCommentsList()) {
            commentLikeRepository.deleteByComment(comment);
        }
        commentRepository.deleteByPost(post);
        postRepository.delete(post);

        accessLogService.logByLoginId(loginId, "POST_DELETE", ip);
        return Map.of("status", "success");
    }

    @Transactional
    public Post toggleLike(Long id, String loginId, String ip) {
        Post post = postRepository.findById(id).orElseThrow();
        Member member = memberRepository.findByLoginId(loginId).orElseThrow();

        Optional<PostLike> existingLike = postLikeRepository.findByMemberAndPost(member, post);

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            post.setLikes(Math.max(0, post.getLikes() - 1));
        } else {
            PostLike newLike = new PostLike();
            newLike.setMember(member);
            newLike.setPost(post);
            postLikeRepository.save(newLike);
            post.setLikes(post.getLikes() + 1);
            accessLogService.logByLoginId(loginId, "LIKE", ip);
        }

        postRepository.save(post);

        entityManager.flush();
        entityManager.clear();

        Post updatedPost = postRepository.findById(id).orElseThrow();
        Member freshMember = memberRepository.findByLoginId(loginId).orElseThrow();
        syncLikedStatus(updatedPost, freshMember);
        return updatedPost;
    }

    @Transactional
    public Post addComment(Long postId, Map<String, String> payload, String ip) {
        Post post = postRepository.findById(postId).orElseThrow();
        String loginId = payload.get("loginId");
        String parentId = payload.get("parentId");
        Member member = memberRepository.findByLoginId(loginId).orElseThrow();

        Comment comment = new Comment();
        comment.setContent(payload.get("content"));
        comment.setAuthor(member.getName());
        comment.setLoginId(member.getLoginId());
        comment.setStudentId(member.getStudentId());
        comment.setProfileImage(member.getProfileImage());
        comment.setDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM.dd HH:mm")));
        comment.setPost(post);

        if (parentId != null && !parentId.isEmpty()) {
            Comment parent = commentRepository.findById(Long.parseLong(parentId)).orElseThrow();
            comment.setParent(parent);
            comment.setReply(true);
        }

        commentRepository.save(comment);
        accessLogService.logByLoginId(loginId, "COMMENT_CREATE", ip);

        entityManager.flush();
        entityManager.clear();

        Post updatedPost = postRepository.findById(postId).orElseThrow();
        Member freshMember = memberRepository.findByLoginId(loginId).orElseThrow();
        syncLikedStatus(updatedPost, freshMember);
        return updatedPost;
    }

    @Transactional
    public Post deleteComment(Long postId, Long commentId, String loginId, String ip) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        commentLikeRepository.deleteByComment(comment);
        commentRepository.delete(comment);

        accessLogService.logByLoginId(loginId, "COMMENT_DELETE", ip);

        entityManager.flush();
        entityManager.clear();

        Post post = postRepository.findById(postId).orElseThrow();
        Member freshMember = memberRepository.findByLoginId(loginId).orElse(null);
        syncLikedStatus(post, freshMember);
        return post;
    }

    @Transactional
    public Post toggleCommentLike(Long postId, Long commentId, String loginId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        Member member = memberRepository.findByLoginId(loginId).orElseThrow();

        Optional<CommentLike> existing = commentLikeRepository.findByMemberAndComment(member, comment);

        if (existing.isPresent()) {
            commentLikeRepository.delete(existing.get());
            comment.setLikes(Math.max(0, comment.getLikes() - 1));
        } else {
            CommentLike cl = new CommentLike();
            cl.setMember(member);
            cl.setComment(comment);
            commentLikeRepository.save(cl);
            comment.setLikes(comment.getLikes() + 1);
        }
        commentRepository.save(comment);

        entityManager.flush();
        entityManager.clear();

        Post post = postRepository.findById(postId).orElseThrow();
        Member freshMember = memberRepository.findByLoginId(loginId).orElseThrow();
        syncLikedStatus(post, freshMember);
        return post;
    }

    private void syncLikedStatus(Post post, Member member) {
        if (member == null) return;

        post.setLikedByMe(postLikeRepository.existsByMemberAndPost(member, post));

        if (post.getCommentsList() != null) {
            post.getCommentsList().forEach(comment -> {
                comment.setLikedByMe(commentLikeRepository.existsByMemberAndComment(member, comment));

                if (comment.getReplies() != null) {
                    comment.getReplies().forEach(reply ->
                            reply.setLikedByMe(commentLikeRepository.existsByMemberAndComment(member, reply))
                    );
                }
            });
        }
    }
}
