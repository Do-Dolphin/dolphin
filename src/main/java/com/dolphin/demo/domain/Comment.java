package com.dolphin.demo.domain;

import com.dolphin.demo.dto.request.CommentRequestDto;
import lombok.*;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.List;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class Comment extends Timestamped {

    @Id
    @Column(name = "comment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    private String title;


    @Column(nullable = false)
    private String content;


    @OneToMany(mappedBy = "comment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CommentImage> imageList;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(nullable = false)
    private int star;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;


    public Comment(CommentRequestDto commentRequestDto, List<CommentImage> imageList) {
        this.title = commentRequestDto.getTitle();
        this.content = commentRequestDto.getContent();
        this.star = commentRequestDto.getStar();
        this.imageList = imageList;
    }

    public Comment(CommentRequestDto commentRequestDto, Place place, Member member) {
        this.place = place;
        this.member = member;
        this.title = commentRequestDto.getTitle();
        this.content = commentRequestDto.getContent();
        this.star = commentRequestDto.getStar();
    }


    public void update(CommentRequestDto commentRequestDto) {
        this.title = commentRequestDto.getTitle();
        this.content = commentRequestDto.getContent();
        this.star = commentRequestDto.getStar();
    }


}
