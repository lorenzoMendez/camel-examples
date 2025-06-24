package com.ibm.camel.web;

import org.apache.camel.ProducerTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class FileProcessorController {
	
	private final ProducerTemplate template;
	
	public FileProcessorController(ProducerTemplate template) {
		this.template = template;
	}
	
	@GetMapping
	public void test() {
		template.sendBody("direct:startFileProcessing", "start");
	}
}
