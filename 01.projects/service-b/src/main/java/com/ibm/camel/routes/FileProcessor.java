package com.ibm.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ibm.camel.repository.FileContentRepository;
import com.ibm.camel.routes.aggregation.BatchAggregationStrategy;
import com.ibm.camel.routes.processor.BatchProcessor;

@Component
public class FileProcessor extends RouteBuilder {

  @Value(value = "${file.source.name}")
  private String fileName;

  public final FileContentRepository fileContentRepository;

  public FileProcessor(FileContentRepository fileContentRepository) {
    this.fileContentRepository = fileContentRepository;
  }

  @Override
  public void configure() throws Exception {

    fromF("file:%s?fileName=%s&noop=true&idempotent=true", "{{file.source.directory}}", fileName)
        .routeId("autoFileRoute")
        .autoStartup(false)
        .split(body().tokenize("\n"))
        	.streaming()
        	.aggregate(constant(true), new BatchAggregationStrategy(10, fileName))
        		.completionSize(constant(10))
        		.completionTimeout(constant(2000))
        		// .transform().body()
        		.process(new BatchProcessor(fileContentRepository))
        	.end()
        .log("Processing batch....")
	.end()
    .log("File processed.");

    from("direct:startFileProcessing")
        .to("controlbus:route?routeId=autoFileRoute&action=start")
        .log("Process starting...");
  }
}
