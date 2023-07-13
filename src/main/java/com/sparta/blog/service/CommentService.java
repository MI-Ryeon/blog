package com.sparta.blog.service;

import com.sparta.blog.dto.CommentRequestDto;
import com.sparta.blog.dto.CommentResponseDto;
import com.sparta.blog.entity.*;
import com.sparta.blog.repository.CommentLikeRepository;
import com.sparta.blog.repository.CommentRepository;
import com.sparta.blog.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.RejectedExecutionException;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final PostService postService;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;

    public CommentResponseDto createComment(CommentRequestDto requestDto, User user) {
        Post post = postService.findPost(requestDto.getPostId());
        Comment comment = new Comment(requestDto.getBody());
        comment.setUser(user);
        comment.setPost(post);

        var savedComment = commentRepository.save(comment);

        return new CommentResponseDto(savedComment);
    }

    public void deleteComment(Long id, User user) {
        Comment comment = commentRepository.findById(id).orElseThrow();

        // 요청자가 운영자 이거나 댓글 작성자(post.user) 와 요청자(user) 가 같은지 체크
        if (!user.getRole().equals(UserRoleEnum.ADMIN) && !comment.getUser().equals(user)) {
            throw new RejectedExecutionException();
        }

        commentRepository.delete(comment);
    }

    @Transactional
    public CommentResponseDto updateComment(Long id, CommentRequestDto requestDto, User user) {
        Comment comment = commentRepository.findById(id).orElseThrow();

        // 요청자가 운영자 이거나 댓글 작성자(post.user) 와 요청자(user) 가 같은지 체크
        if (!user.getRole().equals(UserRoleEnum.ADMIN) && !comment.getUser().equals(user)) {
            throw new RejectedExecutionException();
        }

        comment.setBody(requestDto.getBody());

        return new CommentResponseDto(comment);
    }

    public void likeComment(UserDetailsImpl userDetails, Long id) {
        User user = userDetails.getUser();

        if (user == null) {
            throw new RejectedExecutionException("사용자를 찾을 수 없습니다.");
        }

        Comment comment = commentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (user.getId().equals(comment.getUser().getId())) {
            throw new RejectedExecutionException("본인의 게시글엔 좋아요를 할 수 없습니다.");
        }

        CommentLike commentLike = commentLikeRepository.findByUserAndComment(user, comment);
        if (commentLike != null) {
            throw new RejectedExecutionException("이미 좋아요를 눌렀습니다.");
        }

        commentLikeRepository.save(new CommentLike(user, comment));
    }

    public void deleteLikeComment(UserDetailsImpl userDetails, Long id) {
        User user = userDetails.getUser();

        Comment comment = commentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (user == null) {
            throw new RejectedExecutionException("사용자를 찾을 수 없습니다.");
        }

        CommentLike commentLike = commentLikeRepository.findByUserAndComment(user, comment);

        if (commentLike == null) {
            throw new RejectedExecutionException("좋아요를 누르지 않았습니다.");
        }

        if (this.checkValidUser(user, commentLike)) {
            throw new RejectedExecutionException("본인의 좋아요만 취소할 수 있습니다.");
        }

        commentLikeRepository.delete(commentLike);
    }

    private boolean checkValidUser(User user, CommentLike commentLike) {
        boolean result = !(user.getId().equals(commentLike.getUser().getId())) && !(user.getRole().equals(UserRoleEnum.ADMIN));
        return result;
    }
}
