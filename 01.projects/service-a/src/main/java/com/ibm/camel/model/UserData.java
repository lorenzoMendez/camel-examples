package com.ibm.camel.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class UserData implements Serializable {

  private static final long serialVersionUID = 5381208589779483624L;

  private Integer count;

  private String name;

  private List<Country> country;
}
