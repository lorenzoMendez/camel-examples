package com.demo.order.service.model;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Products implements Serializable {
	
	private static final long serialVersionUID = -997971155086796158L;
	
    private List<Product> products;
	
}
