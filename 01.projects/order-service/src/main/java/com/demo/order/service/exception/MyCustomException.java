package com.demo.order.service.exception;

public class MyCustomException extends RuntimeException {

  private static final long serialVersionUID = 6173148504313767685L;
  
  public MyCustomException(String message) {
	  super(message);
  }
	
	
}
