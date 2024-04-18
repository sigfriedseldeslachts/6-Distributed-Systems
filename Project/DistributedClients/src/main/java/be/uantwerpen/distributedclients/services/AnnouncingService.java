package be.uantwerpen.distributedclients.services;

import be.uantwerpen.distributedclients.models.Node;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.*;

@Service
public class AnnouncingService {

    private DatagramSocket socket;
    private InetSocketAddress group;
    private byte[] buffer;

    private Logger logger = LoggerFactory.getLogger(AnnouncingService.class);

    public AnnouncingService() throws Exception {
        InetAddress address = InetAddress.getByName("230.0.0.0");
        this.group = new InetSocketAddress(address, 6789);
        NetworkInterface netIf = NetworkInterface.getByName("virbr0");
        this.socket = new MulticastSocket(6789);
        this.socket.joinGroup(new InetSocketAddress(address, 0), netIf);

        Node node = new Node("test", InetAddress.getLoopbackAddress());
        setBufferUsingNode(node);
    }

    public void announce() {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group);
        try {
            socket.send(packet);
            logger.info("Sent packet: " + new String(buffer));
        } catch (Exception e) {
            logger.error("Failed to send packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setBufferUsingNode(Node node) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        this.buffer = objectMapper.writeValueAsString(node).getBytes();
    }

}
