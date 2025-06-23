package com.ibm.camel.model.response;

import java.io.Serializable;
import java.util.List;

import com.ibm.camel.model.Country;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
public class UserResponse implements Serializable {

  private static final long serialVersionUID = 8446694530101400269L;

  private String name;

  private Integer age;

  private List<Country> countries;
}
