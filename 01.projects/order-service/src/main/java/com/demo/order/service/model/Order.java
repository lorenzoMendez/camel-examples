package com.demo.order.service.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Order implements Serializable {

  private static final long serialVersionUID = 5078080256948547044L;
	
	private Client client;
	
	private List<Product> products;
	
	private BigDecimal total;
	
	private String date;
	
}
