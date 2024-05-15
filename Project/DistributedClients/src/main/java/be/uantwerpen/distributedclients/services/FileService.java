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

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
@EnableAsync
public class FileService {
    private final Logger logger = LoggerFactory.getLogger(FileService.class);
    private final InfoService infoService;
    private final HashMap<Integer, File> fileList = new HashMap<>();
    private final HashMap<Integer, File> localFiles = new HashMap<>();
    private Map<Integer, Integer> nodesToStoreFilesOn = new HashMap<>();
    private final RestTemplate restTemplate;
    private final String dataPath;

    public FileService(InfoService infoService, Environment env, RestClient.Builder restClientBuilder) {
        this.infoService = infoService;
        this.dataPath = env.getProperty("app.directory");
        this.restTemplate = new RestTemplateBuilder().build();

        if (this.dataPath == null) {
            throw new IllegalArgumentException("app.directory is not set in application.properties");
        }

        // Create directory if it does not exist
        File directory = new File(this.dataPath);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                logger.info("Created directory: {}", this.dataPath);
            } else {
                logger.error("Failed to create directory: {}", this.dataPath);
            }
        }
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
        //Map<Integer, Integer> map = client.method(HttpMethod.POST).body(new ArrayList<>(fileList.keySet())).retrieve().body(new ParameterizedTypeReference<>() {});
        nodesToStoreFilesOn = client.method(HttpMethod.POST).body(new ArrayList<>(fileList.keySet())).retrieve().body(new ParameterizedTypeReference<>() {});


        for (Integer hashFile : nodesToStoreFilesOn.keySet()) {
            int node = nodesToStoreFilesOn.get(hashFile);
            if (node == infoService.getSelfNode().hashCode()) {
                // Put all local files into one hashmap
                localFiles.put(hashFile, fileList.get(hashFile));
                continue;
            }

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
                logger.debug("File uploaded successfully: {}", file.getName());
            } else {
                logger.debug("Failed to upload file. Status code: {}. File name: {}", responseEntity.getStatusCode(), file.getName());
            }
        }

    }

    @Async
    @Scheduled(fixedRate = 5000)
    public void update(){
        File directory = new File(this.dataPath);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                fileList.put(HashingFunction.getHashFromString(file.getName()), file);
            }
        }
        // Loop trough local files and see if files got removed by checking it against the current files
        for (Integer fileHash: localFiles.keySet()) {
            if (fileList.get(fileHash) == null) {
                // Get node its saved on
                String nodeAddress = infoService.getNodes().get(nodesToStoreFilesOn.get(fileHash)).getSocketAddress();
                // Get the filename
                String fileName = localFiles.get(fileHash).getName();
                // Send delete request
                RestClient client = RestClient.builder()
                        .baseUrl("http://" + nodeAddress + "/files/replication/")
                        .build();
                client.method(HttpMethod.DELETE)
                        .uri(uriBuilder -> uriBuilder.path(fileName).build()) // Append the file name to the base URL
                        .retrieve();
            }
        }
    }

    public void store(MultipartFile file) throws IOException {
        File targetFile = new File(this.dataPath, Objects.requireNonNull(file.getOriginalFilename()));
        logger.info("Storing file: {}", targetFile.getAbsoluteFile());

        InputStream initialStream = file.getInputStream();
        byte[] buffer = new byte[initialStream.available()];
        initialStream.read(buffer);

        try (OutputStream outStream = new FileOutputStream(targetFile)) {
            outStream.write(buffer);
        } catch (Exception e) {
            logger.error("Failed to store file: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    public void remove(String fileName) {
        // Remove file from file list
        Integer fileHash = HashingFunction.getHashFromString(fileName);
        fileList.remove(fileHash);
        // Remove file from directory
        File fileToRemove = new File(this.dataPath, fileName);
        // Check if the file exists
        if (fileToRemove.exists()) {
            // Attempt to delete the file
            boolean isDeleted = fileToRemove.delete();
            // Check if the file was successfully deleted
            if (isDeleted) {
                System.out.println("File " + fileName + " has been successfully deleted.");
            } else {
                System.out.println("Failed to delete file " + fileName + ".");
            }
        } else {
            System.out.println("File " + fileName + " does not exist in the specified directory.");
        }
    }
}
