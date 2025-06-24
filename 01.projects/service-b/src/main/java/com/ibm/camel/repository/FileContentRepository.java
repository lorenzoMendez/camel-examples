package com.ibm.camel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ibm.camel.entity.FileContentEntity;

@Repository
public interface FileContentRepository extends JpaRepository<FileContentEntity, Long> {
	
}
