package be.uantwerpen.distributedclients;

import be.uantwerpen.distributedclients.services.AnnouncingService;
import be.uantwerpen.distributedclients.services.InfoService;
import be.uantwerpen.distributedclients.services.ReceiveMulticastOfNewNode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class DistributedClientsApplication {

	public static void main(String[] args) throws Exception {
		// Did this hackery because otherwise JADE classes can't cast Spring classes.
		// What I found is due to Spring proxying, don't understand it fully tbh
		// More at: https://docs.spring.io/spring-framework/reference/core/aop/proxying.html
		System.setProperty("spring.devtools.restart.enabled", "false");
		SpringApplication.run(DistributedClientsApplication.class, args);
	}
}
