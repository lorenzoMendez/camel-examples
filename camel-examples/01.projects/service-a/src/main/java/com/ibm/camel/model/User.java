package com.ibm.camel.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

  private static final long serialVersionUID = 8133241453494705264L;

  private String name;

  private Integer age;

  private Long count;
}
