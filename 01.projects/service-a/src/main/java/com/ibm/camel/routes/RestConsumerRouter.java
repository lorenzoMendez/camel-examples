package com.ibm.camel.routes;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
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

  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * This implementation consumes two APIs sequentially and stores the result in the Property for
   * further processing.
   *
   * @throws Exception.
   */
  // @Override
  public void configure_original() throws Exception {

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

  /**
   * This implementation consumes two APIs in parallel and by implementing aggregationStrategy the
   * responses are processed as the APIs respond.
   */
  @Override
  public void configure() throws Exception {

    from("direct:start")
        .log("Header: ${headers.name}")
        // Definir los endpoints en propiedades
        .setProperty("apiAEndpoint", simple(END_POINT_A))
        .setProperty("apiBEndpoint", simple(END_POINT_B))

        // Enviar en paralelo ambas peticiones
        .recipientList(simple("${exchangeProperty.apiAEndpoint}, ${exchangeProperty.apiBEndpoint}"))
        .parallelProcessing()
        .timeout(3000)
        .aggregationStrategy(new ApiAggregationStrategy())
        .end();
  }

  /**
   * Implements a strategy to obtain API response results.
   */
  class ApiAggregationStrategy implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

      if (oldExchange == null) {
        var response = new UserResponse(null, null, null);
        updateResponse(response, newExchange);
        newExchange.getIn().setBody(response);
        return newExchange;
      }
      var response = oldExchange.getIn().getBody(UserResponse.class);
      updateResponse(response, newExchange);
      newExchange.getIn().setBody(response);

      return oldExchange;
    }

    private void updateResponse(UserResponse response, Exchange exchange) {
      String uri = exchange.getProperty(Exchange.TO_ENDPOINT, String.class);
      String body = exchange.getIn().getBody(String.class);
      log.info("body: {}", body);
      try {
        if (uri.contains("nationalize")) {
          UserData userData;
          userData = objectMapper.readValue(body, UserData.class);
          response.setName(userData.getName());
          response.setCountries(userData.getCountry());
        } else {
          User user = objectMapper.readValue(body, User.class);
          response.setAge(user.getAge());
        }
      } catch (Exception e) {
        log.error("Error deserializing json to java object: ", e);
      }
    }
  }
}
