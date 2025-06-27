package com.demo.order.service.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class Client implements Serializable {
	
	private static final long serialVersionUID = -6664399968336294133L;

    private String name;
	
	private String address;
	
	private String email;
	
}
