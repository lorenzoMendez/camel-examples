package com.ibm.camel.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomErrorException extends RuntimeException {
	
	private static final long serialVersionUID = 8009728005229379481L;

    private String message;
    
    private String description;
	
	public CustomErrorException(String message, String description) {
		super(message);
		this.description = description;
	}
	
}
