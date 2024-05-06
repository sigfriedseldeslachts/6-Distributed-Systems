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
		SpringApplication.run(DistributedClientsApplication.class, args);
	}
}
