package be.uantwerpen.distributedclients;

import be.uantwerpen.distributedclients.services.FileService;
import be.uantwerpen.distributedclients.services.InfoService;
import be.uantwerpen.distributedclients.services.ReceiveMulticastOfNewNode;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class Config implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ReceiveMulticastOfNewNode receiveMulticastOfNewNode(InfoService infoService, FileService fileService) throws Exception {
        return new ReceiveMulticastOfNewNode(infoService, fileService);
    }

    @Bean
    public AgentContainer agentContainer() throws StaleProxyException {
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        AgentContainer mainContainer = runtime.createMainContainer(profile);

        AgentController agentController = mainContainer.createNewAgent(
                "syncAgent",
                "be.uantwerpen.distributedclients.agents.SyncAgent",
                new Object[]{applicationContext.getBean(FileService.class)});
        agentController.start();

        return mainContainer;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}

