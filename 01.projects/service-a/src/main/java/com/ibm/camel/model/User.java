package com.ibm.camel.model;

import java.io.Serializable;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class User implements Serializable {

  private static final long serialVersionUID = 8133241453494705264L;

  private String name;

  private Integer age;

  private Long count;
}
