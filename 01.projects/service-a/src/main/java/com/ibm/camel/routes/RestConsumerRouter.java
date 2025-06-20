package com.ibm.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

import com.ibm.camel.model.AuditLogEventDto;
import com.ibm.camel.model.User;
import com.ibm.camel.model.UserData;
import com.ibm.camel.model.response.UserResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RestConsumerRouter extends RouteBuilder {

  private final String END_POINT_A = "{{camel.resources.nationalize.url}}&bridgeEndpoint=true";

  private final String END_POINT_B = "{{camel.resources.agify.url}}&bridgeEndpoint=true";

  @Override
  public void configure() throws Exception {

    restConfiguration().component("servlet").bindingMode(RestBindingMode.json);

    // Definir endpoint REST
    from("direct:processRequest")
        // from("timer:processRequestTimer?period=10000")
        .routeId("service-request-Id")
        .log("${headers.name}")
        .toD(END_POINT_A)
        .unmarshal()
        .json(JsonLibrary.Jackson, UserData.class)
        .log("${body}")
        .setProperty("serviceAResponse", body())
        // .log("ExchangeProperty: ${exchangeProperty}")
        .toD(END_POINT_B)
        .unmarshal()
        .json(JsonLibrary.Jackson, User.class)
        .log("${body}")
        .setProperty("serviceBResponse", body())
        // .log("ExchangeProperty: ${exchangeProperty}")
        .process(
            exchange -> {
              var userByCountry = exchange.getProperty("serviceAResponse", UserData.class);
              log.info("process -> {}", userByCountry);
              User user = exchange.getProperty("serviceBResponse", User.class);
              log.info("process -> {}", user);
              var response =
                  new UserResponse(user.getName(), user.getAge(), userByCountry.getCountry());
              exchange.getIn().setBody(response);
            })
        .log("Final body: ${body}")
        .wireTap("direct:audit-logging-sender")
        .copy();

    from("direct:audit-logging-sender")
        .routeId("audit-logging-sender-router")
        .log("Audit logging sender")
        .process(
            exchange -> {
              var response = exchange.getIn().getBody(UserResponse.class);
              log.info("Data: {}", response);
              var aditDto =
                  new AuditLogEventDto(
                      "GET_USER",
                      "Retrievving user data from APIs. Name: " + response.getName(),
                      "SUCCESS");
              exchange.getIn().setBody(aditDto);
            })
        .marshal()
        .json()
        .log("${body}")
        .to("activemq:audit-logging-channel");
  }
}
