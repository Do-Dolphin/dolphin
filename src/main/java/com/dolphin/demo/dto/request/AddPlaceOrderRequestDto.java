package com.dolphin.demo.dto.request;

import lombok.Getter;

@Getter
public class AddPlaceOrderRequestDto {
    private String title;
    private String content;
    private String type;
    private String address;
}
