package com.demo.order.service.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ProductProcessable implements Serializable {

  private static final long serialVersionUID = -406828744615813951L;

  private String reason;
  
  private EnumProductStatus status;
  
  private Product product;
	
}
