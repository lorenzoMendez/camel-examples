package com.demo.order.service.model;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Product implements Serializable {
	
	private static final long serialVersionUID = -4486626990303981604L;

    private String code;
	
	private String name;
	
	private Integer quantity;
	
	private BigDecimal price;
}
