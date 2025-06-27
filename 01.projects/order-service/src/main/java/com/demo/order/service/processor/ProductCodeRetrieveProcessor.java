package com.demo.order.service.processor;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.demo.order.service.model.Order;
import com.demo.order.service.model.Product;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProductCodeRetrieveProcessor implements Processor {

  @Override
  public void process(Exchange exchange) throws Exception {
    var order = exchange.getIn().getBody(Order.class);
    List<String> codes =
        order.getProducts().stream().map(Product::getCode).collect(Collectors.toList());
    log.info("Product codes: {}", codes);
    exchange.setProperty("productCodes", codes);
  }
}
