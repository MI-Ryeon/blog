package com.sparta.blog.service;

import com.sparta.blog.dto.PostListResponseDto;
import com.sparta.blog.dto.PostRequestDto;
import com.sparta.blog.dto.PostResponseDto;
import com.sparta.blog.entity.Post;
import com.sparta.blog.entity.PostLike;
import com.sparta.blog.entity.User;
import com.sparta.blog.entity.UserRoleEnum;
import com.sparta.blog.repository.PostLikeRepository;
import com.sparta.blog.repository.PostRepository;
import com.sparta.blog.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    public PostResponseDto createPost(PostRequestDto requestDto, User user) {
        Post post = new Post(requestDto);
        post.setUser(user);

        postRepository.save(post);

        return new PostResponseDto(post);
    }

    public PostListResponseDto getPosts() {
        List<PostResponseDto> postList = postRepository.findAll().stream()
                .map(PostResponseDto::new)
                .collect(Collectors.toList());

        return new PostListResponseDto(postList);
    }

    public PostResponseDto getPostById(Long id) {
        Post post = findPost(id);

        return new PostResponseDto(post);
    }

    public void deletePost(Long id, User user) {
        Post post = findPost(id);

        // 게시글 작성자(post.user) 와 요청자(user) 가 같은지 또는 Admin 인지 체크 (아니면 예외발생)
        if (!(user.getRole().equals(UserRoleEnum.ADMIN) || post.getUser().equals(user))) {
            throw new RejectedExecutionException();
        }

        postRepository.delete(post);
    }

    @Transactional
    public PostResponseDto updatePost(Long id, PostRequestDto requestDto, User user) {
        Post post = findPost(id);

        // 게시글 작성자(post.user) 와 요청자(user) 가 같은지 또는 Admin 인지 체크 (아니면 예외발생)
        if (!(user.getRole().equals(UserRoleEnum.ADMIN) || post.getUser().equals(user))) {
            throw new RejectedExecutionException();
        }

        post.setTitle(requestDto.getTitle());
        post.setContent(requestDto.getContent());

        return new PostResponseDto(post);
    }

    public Post findPost(long id) {
        return postRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("선택한 게시글은 존재하지 않습니다.")
        );
    }

    public void likePost(UserDetailsImpl userDetails, Long id) {
        User user = userDetails.getUser();

        if (user == null) {
            throw new RejectedExecutionException("사용자를 찾을 수 없습니다.");
        }

        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (user.getId().equals(post.getUser().getId())) {
            throw new RejectedExecutionException("본인의 게시글엔 좋아요를 할 수 없습니다.");
        }

        PostLike postLike = postLikeRepository.findByUserAndPost(user, post);
        if (postLike != null) {
            throw new RejectedExecutionException("이미 좋아요를 눌렀습니다.");
        }

        postLikeRepository.save(new PostLike(user, post));
    }

    public void deleteLikePost(UserDetailsImpl userDetails, Long id) {
        User user = userDetails.getUser();

        if (user == null) {
            throw new RejectedExecutionException("사용자를 찾을 수 없습니다.");
        }

        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        PostLike postLike = postLikeRepository.findByUserAndPost(user, post);
        if (postLike == null) {
            throw new RejectedExecutionException("좋아요를 누르지 않았습니다.");
        }

        if (this.checkValidUser(user, postLike)) {
            throw new RejectedExecutionException("본인의 좋아요만 취소할 수 있습니다.");
        }

        postLikeRepository.delete(postLike);
    }

    private boolean checkValidUser(User user, PostLike postLike) {
        boolean result = !(user.getId().equals(postLike.getUser().getId())) && !(user.getRole().equals(UserRoleEnum.ADMIN));
        return result;
    }
}