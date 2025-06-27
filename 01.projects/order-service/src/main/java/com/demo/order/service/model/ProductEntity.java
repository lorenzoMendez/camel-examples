package com.demo.order.service.model;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@Table(name = "product")
public class ProductEntity implements Serializable {

  private static final long serialVersionUID = -781533210938142881L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String code;

  @Column(name = "base_price")
  private BigDecimal price;

  @Column(name = "tax_rate")
  private BigDecimal tax;

  @Column(name = "stock_quantity")
  private Integer stockQuantity;

  @Column(name = "is_active")
  private Boolean isActive;
}
