package com.ibm.camel.routes.aggregation;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

import com.ibm.camel.entity.FileContentEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BatchAggregationStrategy implements AggregationStrategy {

  private final int batchSize;

  private final String fileName;
  
  private Integer lineIndex;

  public BatchAggregationStrategy(int batchSize, String fileName) {
    this.batchSize = batchSize;
    this.fileName = fileName;
    this.lineIndex = 0;
  }

  @Override
  public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
    List<FileContentEntity> batch =
        (oldExchange == null
            ? new ArrayList<FileContentEntity>(batchSize)
            : oldExchange.getIn().getBody(List.class));
    log.info("Batch size: {}", batchSize);

    if (oldExchange == null) {
      log.debug("========> New batch...");
    }

    var fileContentEntity = processLine(newExchange.getIn().getBody(String.class), lineIndex++);
    log.debug("========> {}", fileContentEntity);

    batch.add(fileContentEntity);
    log.debug("Current batch index: {}", batch.size());
    newExchange.getIn().setBody(batch);

    return newExchange;
  }

  private FileContentEntity processLine(String line, int lineNumber) {
    var entity = new FileContentEntity();
    entity.setContent(line);
    entity.setFileName(fileName);
    entity.setLineNumber(lineNumber);
    entity.setStatus("SUCCESS");
    return entity;
  }
}
