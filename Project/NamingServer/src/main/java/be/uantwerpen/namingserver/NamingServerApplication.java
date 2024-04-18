package be.uantwerpen.namingserver;

import be.uantwerpen.namingserver.services.DiscoveryService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.net.SocketException;

@SpringBootApplication
public class NamingServerApplication {

    public static void main(String[] args) throws SocketException {
        ApplicationContext context = SpringApplication.run(NamingServerApplication.class, args);

        DiscoveryService discoveryService = context.getBean(DiscoveryService.class);
        discoveryService.start();
    }

}
