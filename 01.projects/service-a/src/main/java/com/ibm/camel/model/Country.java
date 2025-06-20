package com.ibm.camel.model;

import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Country implements Serializable {

  private static final long serialVersionUID = -3082557821001824100L;

  @JsonProperty("country_id")
  private String countryId;

  private BigDecimal probability;
}
