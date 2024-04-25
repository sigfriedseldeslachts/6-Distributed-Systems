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
		ApplicationContext context = SpringApplication.run(DistributedClientsApplication.class, args);

		ReceiveMulticastOfNewNode rec = new ReceiveMulticastOfNewNode(context.getBean(InfoService.class));
		rec.start();

		AnnouncingService announcingService = new AnnouncingService(context.getBean(InfoService.class));
		while (true) {
			announcingService.announce();
			// Shitty wait for a second
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
