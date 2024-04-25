package be.uantwerpen.distributedclients.services;

import be.uantwerpen.distributedclients.models.Node;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.*;

/*
    a method that sends multicast message to existing nodes and the naming server
 */

@Service
public class AnnouncingService {

    private final InfoService infoService;
    private DatagramSocket socket;
    private InetSocketAddress group;
    private byte[] buffer;

    private Logger logger = LoggerFactory.getLogger(AnnouncingService.class);

    public AnnouncingService(InfoService infoService, Environment env) throws Exception {
        this.infoService = infoService;

        // Get network interface on which we want to send multicast messages
        NetworkInterface netIf = NetworkInterface.getByName(env.getProperty("app.interface"));

        // Create the own node
        Node selfNode = new Node(InetAddress.getLocalHost().getHostName(), new InetSocketAddress(netIf.getInetAddresses().nextElement(), env.getProperty("server.port", Integer.class, 8000)));
        this.infoService.setNode(selfNode);

        // Setup multicast socket
        InetAddress address = InetAddress.getByName("230.0.0.0");
        this.group = new InetSocketAddress(address, 6789);
        this.socket = new MulticastSocket(6789);
        this.socket.joinGroup(new InetSocketAddress(address, 0), netIf);

        setBufferUsingNode(selfNode); // Set buffer to the node
    }

    public void announce() {
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

}
