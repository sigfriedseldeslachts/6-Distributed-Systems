package be.uantwerpen.distributedclients.services;

import be.uantwerpen.distributedclients.models.Node;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.*;

/*
    a method that sends multicast message to existing nodes and the naming server
 */

@Service
@EnableAsync
public class AnnouncingService {
    private final DatagramSocket socket;
    private final InetSocketAddress group;
    private byte[] buffer;
    private final Logger logger = LoggerFactory.getLogger(AnnouncingService.class);

    private final InfoService infoService;
    private final ReceiveMulticastOfNewNode receiveMulticastOfNewNode;

    public AnnouncingService(InfoService infoService, ReceiveMulticastOfNewNode receiveMulticastOfNewNode, Environment env) throws Exception {
        this.infoService = infoService;
        this.receiveMulticastOfNewNode = receiveMulticastOfNewNode;

        // Show all interfaces
        logger.info("Available network interfaces:");
        NetworkInterface.getNetworkInterfaces().asIterator().forEachRemaining(i -> {
           logger.info(" - {}", i.getName());
        });
        logger.info("Selected interface: {}", env.getProperty("app.interface"));

        // Get network interface on which we want to send multicast messages
        NetworkInterface netIf = NetworkInterface.getByName(env.getProperty("app.interface"));
        String nodeName = env.getProperty("app.nodeName", InetAddress.getLocalHost().getHostName());

        // Get the IPv4 address of the networgitk interface, we do this because I am too fucking lazy to bother with IPv6 with this dumb course
        InetAddress inetAddress = netIf.getInterfaceAddresses().stream()
                .map(InterfaceAddress::getAddress)
                .filter(address -> address instanceof Inet4Address)
                .findFirst()
                .orElseThrow(() -> new UnknownHostException("No IPv4 address found on interface " + netIf.getName()));

        // Create the own node
        Node selfNode = new Node(nodeName, new InetSocketAddress(inetAddress, env.getProperty("server.port", Integer.class, 8000)));
        this.infoService.setSelfNode(selfNode);

        // Setup multicast socket
        InetAddress address = InetAddress.getByName("230.0.0.0");
        this.group = new InetSocketAddress(address, 6789);
        this.socket = new MulticastSocket(6789);
        this.socket.joinGroup(new InetSocketAddress(address, 0), netIf);
    }

    @Async
    @Scheduled(fixedRate = 1000)
    public void announce() throws JsonProcessingException {
        setBufferUsingNode(this.infoService.getSelfNode()); // Set buffer to the node
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group);
        try {
            socket.send(packet);
            logger.info("Sent packet: {}", new String(buffer));
        } catch (Exception e) {
            logger.error("Failed to send packet: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    public void setBufferUsingNode(Node node) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        this.buffer = objectMapper.writeValueAsString(node).getBytes();
    }

    @PreDestroy
    public void destroy() throws InterruptedException, JsonProcessingException {
        this.infoService.getSelfNode().setLeaving();
        logger.info("Node is leaving, sending leaving messages, waiting for 5 seconds before exiting...");
        for (int i = 0; i < 5; i++) {
            announce();
            Thread.sleep(1000);
        }
    }
}
