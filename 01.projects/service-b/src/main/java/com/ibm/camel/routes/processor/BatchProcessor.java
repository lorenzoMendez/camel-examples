package com.ibm.camel.routes.processor;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.ibm.camel.entity.FileContentEntity;
import com.ibm.camel.repository.FileContentRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BatchProcessor implements Processor {

  public final FileContentRepository fileContentRepository;

  public BatchProcessor(FileContentRepository fileContentRepository) {
    this.fileContentRepository = fileContentRepository;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    List<FileContentEntity> items = exchange.getIn().getBody(List.class);
    log.debug("items: {}", items);
    log.info("Batch processed with {} items", items.size());
    fileContentRepository.saveAll(items);
    log.debug("Saved items in batch...");
  }
}
