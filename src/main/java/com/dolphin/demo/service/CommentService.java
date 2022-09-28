package com.dolphin.demo.service;

import com.dolphin.demo.domain.Comment;
import com.dolphin.demo.domain.CommentImage;
import com.dolphin.demo.domain.Member;
import com.dolphin.demo.domain.Place;
import com.dolphin.demo.dto.request.CommentRequestDto;
import com.dolphin.demo.dto.request.ImageRequestDto;
import com.dolphin.demo.dto.response.CommentResponseDto;
import com.dolphin.demo.exception.CustomException;
import com.dolphin.demo.jwt.UserDetailsImpl;
import com.dolphin.demo.repository.CommentRepository;
import com.dolphin.demo.repository.CommentImageRepository;
import com.dolphin.demo.repository.MemberRepository;
import com.dolphin.demo.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.dolphin.demo.exception.ErrorCode.DO_NOT_MATCH_USER;


@RequiredArgsConstructor
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final AmazonS3Service amazonS3Service;
    private final CommentImageRepository commentImageRepository;
    private final PlaceRepository placeRepository;
    private final MemberRepository memberRepository;


    // 여행지 상세페이지 후기 조회
    public ResponseEntity<List<CommentResponseDto>> getComment(Long place_id) {

        // 예외처리 추가 예정

        // 해당 여행지의 모든 후기를 작성일 기준 최신순으로 불러옴 ,, 수정 예정
        List<Comment> commentList = commentRepository.findAllByPlaceId(place_id);


        // 보여줄 데이터
        List<CommentResponseDto> commentResult = new ArrayList<>();
        // 불러온 모든 후기와 후기별 이미지를 comments에 담기
        for (Comment comments : commentList) {
            // 후기별 이미지 리스트 꺼내오기
            List<CommentImage> imageResult = commentImageRepository.findAllByCommentId(comments.getId());
            List<String> imageList = new ArrayList<>();

            // 꺼내온 이미지들을 imageList에 담기
            for(CommentImage images : imageResult) {
                imageList.add(images.getImageUrl());
            }

            CommentResponseDto commentResponseDto = CommentResponseDto.builder()
                    .comment_id(comments.getId())
                    .place_id(comments.getPlace().getId())
                    .nickname(comments.getMember().getNickname())
                    .title(comments.getTitle())
                    .content(comments.getContent())
                    .imageList(imageList)
                    .star(comments.getStar())
                    .createdAt(comments.getCreatedAt())
                    .modifiedAt(comments.getModifiedAt())
                    .build();
            commentResult.add(commentResponseDto);
        }

        return ResponseEntity.ok().body(commentResult);
    }


    // 후기 등록
    public ResponseEntity<CommentResponseDto> createComment(Long place_id, CommentRequestDto commentRequestDto, List<MultipartFile> multipartFile, UserDetailsImpl userDetails) throws IOException {

        // 로그인한 회원인지 여부 검증
        Member member = memberRepository.findByUsername(userDetails.getUsername()).orElse(null);

        // 여행지 존재 여부 검증
        Place place = placeRepository.findById(place_id)
                .orElseThrow(() -> new IllegalArgumentException("여행지가 존재하지 않습니다."));

        // 제목, 내용, 별점을 저장
        Comment comment = new Comment(commentRequestDto, place, member);
        place.updateStar(comment.getStar(),1);
        commentRepository.save(comment);


        // 이미지 등록하기
        List<String> imageUrlList;
        List<String> imageList = new ArrayList<>();

        imageUrlList = amazonS3Service.upload(multipartFile);
        List<CommentImage> saveImages = new ArrayList<>();
        for (String imageUrl : imageUrlList) {
            CommentImage image = CommentImage.builder()
                    .comment(comment)
                    .imageUrl(imageUrl)
                    .build();
            saveImages.add(image);
            imageList.add(image.getImageUrl());
        }

        commentImageRepository.saveAll(saveImages);
        return ResponseEntity.ok().body(CommentResponseDto.builder()
                .comment_id(comment.getId())
                .place_id(comment.getPlace().getId())
                .nickname(comment.getMember().getNickname())
                .title(comment.getTitle())
                .content(comment.getContent())
                .imageList(imageList)
                .star(comment.getStar())
                .createdAt(comment.getCreatedAt())
                .modifiedAt(comment.getModifiedAt())
                .build());
    }


    // 후기 수정하기
    public ResponseEntity<CommentResponseDto> updateComment(Long comment_id, ImageRequestDto imageRequestDto, List<MultipartFile> multipartFile, UserDetailsImpl userDetails) throws IOException {

        // 로그인한 회원인지 여부 검증
        Member member = memberRepository.findByUsername(userDetails.getUsername()).orElse(null);

        // 후기 존재 여부 검증
        Comment comment = commentRepository.findById(comment_id)
                .orElseThrow(() -> new IllegalArgumentException("후기가 존재하지 않습니다."));

        // 작성자가 맞는지 여부 검증
        if(!(comment.getMember().getId().equals(member.getId()))) {
            throw new CustomException(DO_NOT_MATCH_USER);
        }


        // 해당 후기의 모든 이미지 불러오기
        List<CommentImage> image = commentImageRepository.findAllByCommentId(comment_id);
        comment.getPlace().updateStar(imageRequestDto.getStar()-comment.getStar(),0);


        // 수정된 내용 저장
        CommentRequestDto updateComment = CommentRequestDto.builder()
                .title(imageRequestDto.getTitle())
                .content(imageRequestDto.getContent())
                .star(imageRequestDto.getStar())
                .build();
        comment.update(updateComment);
        commentRepository.save(comment);

        // 이미지 수정 및 재등록 기능
        List<String> imageList = new ArrayList<>();
        // 새로 등록하는 이미지가 없는 경우
        if(multipartFile == null) {
            return ResponseEntity.ok().body(CommentResponseDto.builder()
                    .comment_id(comment.getId())
                    .place_id(comment.getPlace().getId())
                    .nickname(comment.getMember().getNickname())
                    .title(comment.getTitle())
                    .content(comment.getContent())
                    .star(comment.getStar())
                    .createdAt(comment.getCreatedAt())
                    .modifiedAt(comment.getModifiedAt())
                    .build());


            // 새로 등록하는 이미지가 있는 경우
        } else {
            for (int i=0; i<image.size(); i++) {
                /**
                 * 1. image의 ImageUrl : 기존에 등록된 이미지
                 * 2. imageRequestDto의 ExistUrlList : 기존 이미지 중, 수정할 때 삭제되지 않고 남아있는 이미지
                 * 3. 1, 2번을 비교하여 같은 URL이 존재할 경우 imageList에 추가하고 없을 경우 S3와 DB에서 삭제
                 */
                for (int j = 0; j < imageRequestDto.getExistUrlList().size(); j++) {
                    if (image.get(i).getImageUrl().equals(imageRequestDto.getExistUrlList().get(j))) {
                        imageList.add(imageRequestDto.getExistUrlList().get(j));
                    } else {
                    // S3 저장소에 있는 이미지 삭제하기
                        amazonS3Service.deleteFile(image.get(i).getImageUrl().substring(image.get(i).getImageUrl().lastIndexOf("/") + 1));

                        // DB 이미지 삭제
                        commentImageRepository.delete(image.get(i));
                    }
                }
            }


            // 새로운 이미지 등록
            List<String> imageUrlList;
            imageUrlList = amazonS3Service.upload(multipartFile);
            List<CommentImage> saveImage = new ArrayList<>();

            for (String imageUrls : imageUrlList) {
                CommentImage images = CommentImage.builder()
                        .comment(comment)
                        .imageUrl(imageUrls)
                        .build();
                saveImage.add(images);
                imageList.add(images.getImageUrl());
            }
            commentImageRepository.saveAll(saveImage);
            return ResponseEntity.ok().body(CommentResponseDto.builder()
                    .comment_id(comment.getId())
                    .place_id(comment.getPlace().getId())
                    .nickname(comment.getMember().getNickname())
                    .title(comment.getTitle())
                    .content(comment.getContent())
                    .imageList(imageList)
                    .star(comment.getStar())
                    .createdAt(comment.getCreatedAt())
                    .modifiedAt(comment.getModifiedAt())
                    .build());

        }

    }

    // 후기 삭제하기
    public ResponseEntity<Long> deleteComment(Long id, UserDetailsImpl userDetails) throws IOException {

        // 로그인한 회원인지 여부 검증
        Member member = memberRepository.findByUsername(userDetails.getUsername()).orElse(null);

        // 후기 존재 여부 검증
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("후기가 존재하지 않습니다."));

        // 작성자가 맞는지 여부 검증
        if(!(comment.getMember().getId().equals(member.getId()))) {
            throw new CustomException(DO_NOT_MATCH_USER);
        }


        List<CommentImage> image = commentImageRepository.findAllByCommentId(id);

        // 저장된 이미지가 있으면 S3 저장소에 있는 이미지 삭제하기
        for(int i=0; i<image.size(); i++) {
            amazonS3Service.deleteFile(image.get(i).getImageUrl().substring(image.get(i).getImageUrl().lastIndexOf("/") + 1));
        }
        comment.getPlace().updateStar(comment.getStar()*-1,-1);

        // 후기 삭제
        commentRepository.delete(comment);

        return ResponseEntity.ok().body(id);
    }

}
