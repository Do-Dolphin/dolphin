package com.dolphin.demo.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;


@Builder
@Getter
public class EventRequestDto {

    private String title;
    private String linkUrl;
    private String imageUrl;
    private String period;

}