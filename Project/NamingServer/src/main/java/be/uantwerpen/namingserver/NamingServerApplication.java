package be.uantwerpen.namingserver;

import be.uantwerpen.namingserver.models.Node;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NamingServerApplication {

    public static void main(String[] args) {

        System.out.println(new Node("test").hashCode());

        //SpringApplication.run(NamingServerApplication.class, args);
    }

}
