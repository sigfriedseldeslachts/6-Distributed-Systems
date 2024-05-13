package be.uantwerpen.distributedclients.services;

import be.uantwerpen.distributedclients.utils.HashingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@EnableAsync
public class FileService {

    private Logger logger = LoggerFactory.getLogger(FileService.class);
    private InfoService infoService;
    private HashMap<Integer, File> fileList = new HashMap<>();
    private Map<Integer, Integer> map;

    public FileService(InfoService infoService) {
        this.infoService = infoService;
    }

    // dit is de node die zijn request stuurt naar de receiving node
    public void send() {
        if (infoService.getNamingServerAddress() == null) {
            logger.warn("Naming server address is still not known. Skipping file replication");
            return;
        }

        RestClient client = RestClient.builder()
                .baseUrl(infoService.getNamingServerAddress() + "/files/replication")
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.map = client.method(HttpMethod.POST).body(new ArrayList<>(fileList.keySet())).retrieve().body(new ParameterizedTypeReference<>() {});
        System.out.println(map);

        for (Integer hashFile : map.keySet()) {
            int node = map.get(hashFile);

            if (node != infoService.getSelfNode().hashCode()) {
                String nodeAddress = infoService.getNodes().get(node).getSocketAddress();
                File file = fileList.get(hashFile);
                client = RestClient.builder()
                        .baseUrl("http://" + nodeAddress + "/files/replication/")
                        .defaultHeader("Content-Type", "application/json")
                        .build();
                client.method(HttpMethod.POST).body(file);
            } else {
                System.out.println("SKIP");
            }
        }

    }


    @Async
    @Scheduled(fixedRate = 5000)
    public void update(){
        File directory = new File("C:\\Users\\chloe\\Desktop\\data1");
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                fileList.put(HashingFunction.getHashFromString(file.getName()),file);
            }
        }
    }

    public void store(MultipartFile file) throws IOException {
        File directory = new File("C:\\Users\\chloe\\Desktop\\data1");

        file.transferTo(directory);
    }

}
