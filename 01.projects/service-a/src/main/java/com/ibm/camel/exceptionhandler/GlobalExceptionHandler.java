package com.ibm.camel.exceptionhandler;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.ibm.camel.exception.CustomErrorException;
import com.ibm.camel.model.error.ErrorDetail;
import com.ibm.camel.model.error.ErrorProcessor;

import lombok.extern.slf4j.Slf4j;

/**
 * Exception handler.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(CustomErrorException.class)
  public ResponseEntity<ErrorProcessor> customErrorException(
      CustomErrorException customErrorException) {
    var detail = new ErrorDetail("error", "ERROR", customErrorException.getDescription());
    return new ResponseEntity<ErrorProcessor>(
        new ErrorProcessor(List.of(detail)), HttpStatus.BAD_REQUEST);
  }
}
