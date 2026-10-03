package com.pi.erp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseErrorDTO> handleResourceNotFound(
            ResourceNotFoundException exception
    ) {

        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                exception.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseErrorDTO> handleIllegalArgument(
            IllegalArgumentException exception
    ) {

        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                exception.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

//    -------------Tratamento para erros que não estão especificados aqui
//    -------------ativar apenas ao fim do projeto para segurança
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ResponseErrorDTO> handleException(
//            Exception exception
//    ) {
//
//        ResponseErrorDTO error = new ResponseErrorDTO(
//                HttpStatus.INTERNAL_SERVER_ERROR.value(),
//                "Internal Server Error",
//                "An unexpected error occurred.",
//                LocalDateTime.now()
//        );
//
//        return ResponseEntity
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(error);
//    }
}