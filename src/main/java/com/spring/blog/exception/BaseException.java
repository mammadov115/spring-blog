package com.spring.blog.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter 
public class BaseException extends RuntimeException {
    private final HttpStatus status;

    public BaseException(String message, HttpStatus status){
        super(message);
        this.status = status;
    }
}
