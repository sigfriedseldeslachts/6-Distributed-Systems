package be.uantwerpen.distributedclients.services;

import be.uantwerpen.distributedclients.utils.HashingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
@EnableAsync
public class FileService {

    private final RestClient.Builder restClientBuilder;
    private Logger logger = LoggerFactory.getLogger(FileService.class);
    private InfoService infoService;
    private HashMap<Integer, File> fileList = new HashMap<>();
    private Map<Integer, Integer> map;
    private Environment env;

    public FileService(InfoService infoService, Environment env, RestClient.Builder restClientBuilder) {
        this.infoService = infoService;
        this.env = env;
        this.restClientBuilder = restClientBuilder;
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
                        .baseUrl("http://" + nodeAddress + "/files/replication")
                        .defaultHeader("Content-Type", "application/json")
                        .build();

                ResponseEntity<Void> response = client.post()
                        .contentType(APPLICATION_JSON)
                        .body(file)
                        .retrieve()
                        .onStatus(status -> !status.is2xxSuccessful(), (request, resp) -> logger.error("Failed to upload file: status code: {}", resp.getStatusCode()))
                        .toBodilessEntity();

                logger.info("Sending file " + file.getAbsolutePath());
            } else {
                System.out.println("SKIP");
            }
        }

    }


    @Async
    @Scheduled(fixedRate = 5000)
    public void update(){
        File directory = new File(this.env.getProperty("app.directory"));
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                fileList.put(HashingFunction.getHashFromString(file.getName()),file);
            }
        }
    }

    public void store(MultipartFile file) throws IOException {
        File directory = new File(this.env.getProperty("app.directory") + "\\" + file.getName());
        logger.info("Storing file " + file.getName());
        file.transferTo(directory);
    }

}
