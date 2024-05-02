package be.uantwerpen.namingserver.services;

import be.uantwerpen.namingserver.models.Node;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.*;

@Service
public class DiscoveryService {

    private final String interfaceName = "any";

    private final DatagramSocket socket;
    private final InetAddress group;

    private final NodeService nodeService;

    private Thread listener;

    private final Logger logger = LoggerFactory.getLogger(DiscoveryService.class);

    public DiscoveryService(NodeService nodeService) throws Exception {
        this.nodeService = nodeService;

        this.socket = new DatagramSocket();
        this.group = InetAddress.getByName("230.0.0.0");

        // Launch separate thread to listen for incoming messages
        this.listener = new Thread(new DiscoveryListener(this.socket, this.group, this.interfaceName, nodeService));
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
        private NodeService nodeService;
        private Logger logger = LoggerFactory.getLogger(DiscoveryListener.class);

        public DiscoveryListener(DatagramSocket socket, InetAddress address, String interfaceName, NodeService nodeService) throws SocketException {
            this.logger.info("Creating new DiscoveryListener thread.");
            this.socket = socket;
            this.group = new InetSocketAddress(address, 0);
            this.netIf = NetworkInterface.getByName(interfaceName);
            this.nodeService = nodeService;

            this.logger.info("Done creating new DiscoveryListener thread.");
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
                        logger.info("Received message: " + node.getName());
                    } catch (JsonProcessingException e) {
                        logger.warn("Failed to parse received message: " + received);
                        e.printStackTrace();
                        continue;
                    }

                    // Check if node is already known
                    if (nodeService.getNode(node.getName()) == null) {
                        this.logger.info("Discovered new node: " + node.getName());
                        nodeService.addNode(node);
                    } else {
                        this.logger.info("Node already known: " + node.getName());
                        nodeService.updateLastPing(node.getName());
                    }

                    logger.info(node.getSocketAddress());
                }
            } catch (Exception e) {
                logger.error("Failed to listen for incoming messages: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
