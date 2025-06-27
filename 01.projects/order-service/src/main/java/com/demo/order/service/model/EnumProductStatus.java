package com.demo.order.service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EnumProductStatus {
	
	AVAILABLE("Disponible", true, true),
	NOT_ENOUGH_IN_STOCK("No hay suficientes en stock", true, true),
	OUT_OF_STOCK("No disponible en stock", true, false),
	UNAVAILABLE("No disponible", false, false),
	DISCONTINUED("Descontinuado", false, false),
	PRICE_CHANGED("Cambio de precio", true, true),;
	
	private final String description;
	
	private final boolean active;
	
	private final boolean inStock;
	
}
