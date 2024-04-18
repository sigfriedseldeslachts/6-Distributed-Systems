package be.uantwerpen.distributedclients;

import be.uantwerpen.distributedclients.services.AnnouncingService;
import be.uantwerpen.distributedclients.services.ReceiveMulticastOfNewNode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DistributedClientsApplication {

	public static void main(String[] args) throws Exception {
		//SpringApplication.run(DistributedClientsApplication.class, args);

		ReceiveMulticastOfNewNode rec = new ReceiveMulticastOfNewNode();
		rec.start();

		AnnouncingService announcingService = new AnnouncingService();
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
