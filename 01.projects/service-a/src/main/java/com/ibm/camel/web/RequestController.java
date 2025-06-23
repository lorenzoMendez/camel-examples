package com.ibm.camel.web;

import org.apache.camel.ProducerTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ibm.camel.model.response.UserResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/camel")
public class RequestController {

  /**
   * Producer camel template.
   */
  private final ProducerTemplate template;

  /**
   * Bean injection via constructor.
   * 
   * @param template camel producer template.
   */
  public RequestController(ProducerTemplate template) {
    this.template = template;
  }

  /**
   * Process the router 'direct:processRequest'.
   * 
   * @param name variable used to retrieve the data.
   * @return UserResponse data.
   */
  @GetMapping("/user/{name}")
  public ResponseEntity<?> retrieveUserData(@PathVariable("name") String name) {

    log.info("Name: {}", name);
    
    String route = "direct:start";
    //String route = "direct:processRequest";

    return new ResponseEntity<UserResponse>(
        template.requestBodyAndHeader(
        		route, null, "name", name, UserResponse.class),
        HttpStatus.OK);
  }
}
