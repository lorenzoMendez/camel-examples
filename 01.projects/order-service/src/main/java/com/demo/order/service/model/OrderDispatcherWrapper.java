package com.demo.order.service.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrderDispatcherWrapper implements Serializable {

  private static final long serialVersionUID = -406828744615813951L;

  private Client client;

  private List<ProductProcessable> processedProducts = new ArrayList<ProductProcessable>();

  private List<ProductProcessable> unprocessableProducts = new ArrayList<ProductProcessable>();

  private List<ProductProcessable> backOrderProducts = new ArrayList<ProductProcessable>();

  private BigDecimal tax;

  private BigDecimal totalWithoutTax;

  private BigDecimal total;

  private String date;

  public void addProcessableProduct(ProductProcessable processedProduct) {
    this.processedProducts.add(processedProduct);
  }

  public void addNotProcessableProduct(ProductProcessable unprocessableProduct) {
    this.unprocessableProducts.add(unprocessableProduct);
  }

  public void addBackOrderProducts(ProductProcessable backorderProduct) {
    this.backOrderProducts.add(backorderProduct);
  }
}
