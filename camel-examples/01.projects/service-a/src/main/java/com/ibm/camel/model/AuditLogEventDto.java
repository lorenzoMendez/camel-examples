package com.ibm.camel.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuditLogEventDto implements Serializable {

  private static final long serialVersionUID = 1105023013970189298L;

  private String evenType;

  private String description;

  private String status;
}
