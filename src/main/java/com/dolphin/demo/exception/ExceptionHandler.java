package com.dolphin.demo.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice   // 모든 예외를 한 곳에서 처리할 수 있게 해주는 annotation
public class ExceptionHandler {  // 각각의 예외는 @ExceptionHandler를 통해서 예외클래스를 처리

    //@Valid or @Validated 바인딩 에러시 발생

    @org.springframework.web.bind.annotation.ExceptionHandler(value = { CustomException.class })
    public ResponseEntity<ErrorResponse> handleApiRequestException(CustomException ex) {
        return ErrorResponse.responseEntity(ex.getErrorCode());
    }
}