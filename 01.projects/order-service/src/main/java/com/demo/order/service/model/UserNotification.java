package com.demo.order.service.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserNotification implements Serializable {
	
	private static final long serialVersionUID = 3747823117586437705L;

    private String name;
    
    private String message;
	
	private String email;
	
}
