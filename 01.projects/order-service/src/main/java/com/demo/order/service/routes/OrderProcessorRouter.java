package com.demo.order.service.routes;

import java.sql.SQLException;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.kafka.common.KafkaException;
import org.springframework.stereotype.Component;

import com.demo.order.service.exception.MyCustomException;
import com.demo.order.service.model.Order;
import com.demo.order.service.model.UserNotification;
import com.demo.order.service.model.error.ErrorDetail;
import com.demo.order.service.model.error.ErrorProcessor;
import com.demo.order.service.processor.ProductCodeRetrieveProcessor;
import com.demo.order.service.strategy.ItemProcessorStrategy;
import com.fasterxml.jackson.core.JsonParseException;

import jakarta.xml.bind.MarshalException;
import jakarta.xml.bind.UnmarshalException;

@Component
public class OrderProcessorRouter extends RouteBuilder {

  @Override
  public void configure() throws Exception {

    exceptionHandlerConfiguration();

    from("activemq:order-dispatcher-queue")
        .log("=== OrderDispatcherReceiver ====>")
        .routeId("orderDispatcherQueueId")
        .unmarshal()
        .jacksonXml(Order.class)
        .log("=== Message received from queue: ${body}")
        .setProperty("orderDispatcher", simple("${body}"))
        .wireTap("direct:sendNotification")
        .process(new ProductCodeRetrieveProcessor())
        .log("=== Order: ${exchangeProperty.orderDispatcher}")
        .log("=== Products: ${exchangeProperty.productCodes}")
        .to("bean:productRepository?method=findByCodeIn(${exchangeProperty.productCodes})")
        .setProperty("orderProductEntities", simple("${body}"))
        .choice()
        	.when(simple("${exchangeProperty.orderProductEntities.empty}"))
        		.throwException(new MyCustomException("No se encontraron productos con los codigos dados."))
        	.otherwise()
        		.log("=== Products entities: ${exchangeProperty.orderProductEntities}")
        		.split(simple("${exchangeProperty.orderDispatcher.products}"), new ItemProcessorStrategy())
	        		.log("=== Procesando producto: ${body}")
	        		.end()
	        	.multicast()
	        		.parallelProcessing()
	        		.to("direct:sendToKafka", "direct:sendToActiveMQ")
	        	.end()
        .end();

    from("direct:sendToKafka")
        .log("=== KafkaSender ====>")
        .marshal().json(JsonLibrary.Jackson)
        .routeId("sendToKafkaId")
        .to("kafka:orderProcessingMessage")
        .log("=== Json Messsage sent to Kafka: ======> ${body}");

    from("direct:sendToActiveMQ")
        .log("=== ActiveMQSender ====>")
        .routeId("sendToActiveMQId")
        .marshal().jacksonXml()
        .to("activemq:orderProcessingMessage")
        .log("=== Message published in queue: ======> ${body}");

    from("direct:sendNotification")
        .routeId("=== UserNotificationSender ====>")
        .process(
            exchange -> {
              var client = exchange.getIn().getBody(Order.class).getClient();
              var notification =
                  new UserNotification(
                      client.getName(), "Su pedido esta en atención.", client.getName());
              exchange.getIn().setBody(notification);
            })
        .marshal()
        .json(JsonLibrary.Jackson)
        .to("file:{camel.output.notification.path}?fileName={camel.output.notification.file}")
        .log("=== Notification sent to user: ${body}");
  }

  private void exceptionHandlerConfiguration() {
    
	  onException(MyCustomException.class, JsonParseException.class, SQLException.class, UnmarshalException.class, 
			  MarshalException.class, KafkaException.class, RuntimeException.class)
        .handled(true) // Evita que la excepcion se propague
        .maximumRedeliveries(3) // Reintentos
        .redeliveryDelay(1000) // Delay entre reintentos
        .log("Error processing the message.")
        .to("direct:exceptionHandler");

    from("direct:exceptionHandler")
    	.choice()
	    	.when(header(Exchange.EXCEPTION_CAUGHT).isInstanceOf(MyCustomException.class))
				.log("=== Hubo un error al procesar la query: ${exchangeProperty.CamelExceptionCaught.message}")
				.process(exchange -> {
					var exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, MyCustomException.class);
					buildError(exception.getMessage());
					exchange.getIn().setBody(buildError(exception.getMessage()));
				})
				.log("${body}")
    		.when(header(Exchange.EXCEPTION_CAUGHT).isInstanceOf(SQLException.class))
    			.log("=== Hubo un error al procesar la query: ${exchangeProperty.CamelExceptionCaught.message}")
    			.process(exchange -> {
    				var exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, SQLException.class);
    				buildError(exception.getMessage());
    				exchange.getIn().setBody(buildError(exception.getMessage()));
    			})
    			.log("${body}")
    		.when(header("CamelExceptionCaught").isInstanceOf(JsonParseException.class))
    			.log("=== Hubo un error convertir objeto a json: ${exchangeProperty.CamelExceptionCaught.message}")
    			.process(exchange -> {
    				var exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, JsonParseException.class);
    				buildError(exception.getMessage());
    				exchange.getIn().setBody(buildError(exception.getMessage()));
    			})
    			.log("${body}")
    		.when(header("CamelExceptionCaught").isInstanceOf(MarshalException.class) )
    			.log("=== Hubo un error al convertir objeto: ${exchangeProperty.CamelExceptionCaught.message}")
    			.process(exchange -> {
    				var exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, MarshalException.class);
    				buildError(exception.getMessage());
    				exchange.getIn().setBody(buildError(exception.getMessage()));
    			})
    			.log("${body}")
        	.when(header("CamelExceptionCaught").isInstanceOf(UnmarshalException.class) )
    			.log("=== Hubo un error al convertir objeto: ${exchangeProperty.CamelExceptionCaught.message}")
    			.process(exchange -> {
    				var exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, UnmarshalException.class);
    				buildError(exception.getMessage());
    				exchange.getIn().setBody(buildError(exception.getMessage()));
    			})
    			.log("${body}")
    		.otherwise()
    			.log("=== Hubo un error al procesar el pedido: ${exchangeProperty.CamelExceptionCaught.message}")
    			.process(exchange -> {
    				var exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, RuntimeException.class);
    				buildError(exception.getMessage());
    				exchange.getIn().setBody(buildError(exception.getMessage()));
    			})
    			.log("${body}");
  }
  
  private ErrorProcessor buildError(String message) {
	  var detail = new ErrorDetail();
	  detail.setCode("ERROR01");
	  detail.setDescription(message);
	  detail.setLevel("CRITIC");
	  var errorList = new ErrorProcessor();
	  errorList.setErrors(List.of(detail));
	  return errorList;
  }
  
}
