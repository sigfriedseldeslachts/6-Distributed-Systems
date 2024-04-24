package be.uantwerpen.distributedclients.services;

import be.uantwerpen.distributedclients.models.Node;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.*;

import static be.uantwerpen.distributedclients.utils.HashingFunction.getHashFromString;

@Service
public class ReceiveMulticastOfNewNode {
    private final String interfaceName = "any";

    private final DatagramSocket socket;
    private final InetAddress group;

    private Thread listener;

    public ReceiveMulticastOfNewNode(InfoService infoService) throws Exception {

        this.socket = new DatagramSocket();
        this.group = InetAddress.getByName("230.0.0.0");

        // Launch separate thread to listen for incoming messages
        this.listener = new Thread(new DiscoveryListener(this.socket, this.group, this.interfaceName, infoService));
    }

    public void start() throws SocketException {
        this.listener.start();
    }

    private static class DiscoveryListener extends Thread {
        private DatagramSocket socket;
        private InetSocketAddress group;
        private NetworkInterface netIf;
        private final byte[] buffer = new byte[1000];
        private ObjectMapper objectMapper = new ObjectMapper();
        private Logger logger = LoggerFactory.getLogger(DiscoveryListener.class);
        private final InfoService infoService;

        public DiscoveryListener(DatagramSocket socket, InetAddress address, String interfaceName, InfoService infoService) throws SocketException {
            this.logger.info("Creating new DiscoveryListener thread.");
            this.socket = socket;
            this.group = new InetSocketAddress(address, 0);
            this.netIf = NetworkInterface.getByName(interfaceName);
            this.logger.info("Done creating new DiscoveryListener thread.");
            this.infoService = infoService;
        }

        @Override
        public void run() {
            logger.info("Listening for incoming messages...");

            try {
                socket = new MulticastSocket(6789);
                socket.joinGroup(group, netIf);

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String received = new String(packet.getData(), 0, packet.getLength());

                    // Parse the node
                    Node node = null;
                    try {
                        node = this.objectMapper.readValue(received, Node.class);
                        //logger.info("Received message: " + node.getName());

                        // calculate the hash of the node that sent the multicast message

                        int hashOfNode = getHashFromString(node.getName());
                        infoService.setHashOfNewNode(hashOfNode);

                        logger.info(String.valueOf(hashOfNode));

                    } catch (JsonProcessingException e) {
                        logger.warn("Failed to parse received message: " + received);
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to listen for incoming messages: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}
