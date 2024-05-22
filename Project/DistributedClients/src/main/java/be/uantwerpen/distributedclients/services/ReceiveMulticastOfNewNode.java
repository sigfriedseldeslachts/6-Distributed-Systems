package be.uantwerpen.distributedclients.services;

import be.uantwerpen.distributedclients.models.Node;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.*;
import java.time.LocalDateTime;

@Service
public class ReceiveMulticastOfNewNode {
    private final String interfaceName = "any";
    private final Thread listener;

    public ReceiveMulticastOfNewNode(InfoService infoService, FileService fileService) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress group = InetAddress.getByName("230.0.0.0");

        // Launch separate thread to listen for incoming messages
        this.listener = new Thread(new DiscoveryListener(socket, group, this.interfaceName, infoService, fileService));
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
        private FileService fileService;

        public DiscoveryListener(DatagramSocket socket, InetAddress address, String interfaceName, InfoService infoService, FileService fileService) throws SocketException {
            this.logger.info("Creating new DiscoveryListener thread.");
            this.socket = socket;
            this.group = new InetSocketAddress(address, 0);
            this.netIf = NetworkInterface.getByName(interfaceName);
            this.logger.info("Done creating new DiscoveryListener thread.");
            this.infoService = infoService;
            this.fileService = fileService;
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
                    Node node;
                    try {
                        node = this.objectMapper.readValue(received, Node.class);

                        if (!this.infoService.getNodes().containsKey(node.hashCode())) {
                            this.fileService.clearPreviousLocalFiles();
                        }

                        // Make sure the node is not the current node
                        if (node.hashCode() != this.infoService.getSelfNode().hashCode()) {
                            node.setLastPing(LocalDateTime.now());
                            this.infoService.addNewNode(node);
                            logger.info("Message received, added/updated node: {}", received);
                        }


                        // Update the order of the nodes
                        this.infoService.updateNodeOrder();
                    } catch (Exception e) {
                        logger.warn("Failed to parse received message: {}", received);
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to listen for incoming messages: {}", e.getMessage());
                e.printStackTrace();
            }
        }
    }

}
