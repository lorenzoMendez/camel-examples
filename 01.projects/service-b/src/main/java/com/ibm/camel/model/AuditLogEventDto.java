package com.ibm.camel.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class AuditLogEventDto implements Serializable {

  private static final long serialVersionUID = -4844814053873269921L;

  private String evenType;

  private String description;

  private String status;
}
