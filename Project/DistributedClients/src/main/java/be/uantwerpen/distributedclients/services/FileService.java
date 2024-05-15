package be.uantwerpen.distributedclients.services;

import be.uantwerpen.distributedclients.utils.HashingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.boot.web.client.RestTemplateBuilder;

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
    private final RestTemplate restTemplate;

    public FileService(InfoService infoService, Environment env, RestClient.Builder restClientBuilder) {
        this.infoService = infoService;
        this.env = env;
        this.restClientBuilder = restClientBuilder;
        this.restTemplate = new RestTemplateBuilder().build();
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

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("file", new FileSystemResource(file));
                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                ResponseEntity<String> responseEntity = restTemplate.exchange(
                        "http://" + nodeAddress + "/files/replication",
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );
                if (responseEntity.getStatusCode() == HttpStatus.OK) {
                    System.out.println("File uploaded successfully.");
                } else {
                    System.out.println("Failed to upload file. Status code: " + responseEntity.getStatusCode());
                }
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
        File directory = new File(this.env.getProperty("app.directory") + "\\" + file.getOriginalFilename());
        logger.info("Storing file " + file.getOriginalFilename());
        file.transferTo(directory);
    }

}
