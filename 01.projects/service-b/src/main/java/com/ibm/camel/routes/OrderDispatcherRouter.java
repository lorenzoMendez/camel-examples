package com.ibm.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class OrderDispatcherRouter extends RouteBuilder {

  @Override
  public void configure() throws Exception {
	  
	  from("file:{{camel.input.order.path}}?fileName={{camel.input.order.name}}&noop=true")
	  	.routeId("OrderDispatcherId")
	  	.convertBodyTo(String.class)
	  	.log("${body}")
	  	.to("activemq:order-dispatcher-queue");
  }
}
