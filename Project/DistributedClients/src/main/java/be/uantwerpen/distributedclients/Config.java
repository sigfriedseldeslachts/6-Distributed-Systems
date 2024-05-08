package be.uantwerpen.distributedclients;

import be.uantwerpen.distributedclients.services.AnnouncingService;
import be.uantwerpen.distributedclients.services.FileService;
import be.uantwerpen.distributedclients.services.InfoService;
import be.uantwerpen.distributedclients.services.ReceiveMulticastOfNewNode;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class Config {

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ReceiveMulticastOfNewNode receiveMulticastOfNewNode(InfoService infoService, FileService fileService) throws Exception {
        return new ReceiveMulticastOfNewNode(infoService, fileService);
    }

}
