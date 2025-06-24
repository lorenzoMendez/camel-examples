package com.ibm.camel.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@ToString
@Table(name = "file_content")
public class FileContentEntity implements Serializable {
	
  private static final long serialVersionUID = 5019815763954485020L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(columnDefinition = "line_number")
  private Integer lineNumber;

  @Column(name = "content")
  private String content;

  @Column(name = "status")
  private String status;
  
  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @PrePersist
  public void setCreateAt() {
    createdAt = LocalDateTime.now();
  }
}
