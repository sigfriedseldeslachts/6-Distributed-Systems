package be.uantwerpen.distributedclients.services;

import be.uantwerpen.distributedclients.utils.HashingFunction;
import jakarta.annotation.PreDestroy;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@EnableAsync
public class FileService {
    private final Logger logger = LoggerFactory.getLogger(FileService.class);
    private final InfoService infoService;
    private final HashMap<Integer, File> fileList = new HashMap<>();
    private Map<Integer, Integer> nodesToStoreFilesOn = new HashMap<>();
    private final RestTemplate restTemplate;
    private final String replicatedFilesPath;
    private final String localFilesPath;
    private Set<String> previousLocalFiles = new HashSet<>();

    public FileService(InfoService infoService, Environment env, RestClient.Builder restClientBuilder) {
        this.infoService = infoService;
        this.replicatedFilesPath = env.getProperty("app.replicateddirectory");
        this.localFilesPath = env.getProperty("app.localdirectory");
        this.restTemplate = new RestTemplateBuilder().build();

        if (this.replicatedFilesPath == null) {
            throw new IllegalArgumentException("app.directory is not set in application.properties");
        }

        // Create directory if it does not exist
        File directory = new File(this.replicatedFilesPath);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                logger.info("Created directory: {}", this.replicatedFilesPath);
            } else {
                logger.error("Failed to create directory: {}", this.replicatedFilesPath);
            }
        }
    }

    /**
     * de http request om file te transferen en kopieren op andere node
     */
    public void transferRequest(File file, String nodeAddress) {
        logger.info("Transferring file: {}", file.getName());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file.getAbsolutePath()));
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> responseEntity =  restTemplate.exchange(
                "http://" + nodeAddress + "/files/replication",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            logger.debug("File transfered successfully: {}", file.getName());
        } else {
            logger.debug("Failed to transfer file. Status code: {}. File name: {}", responseEntity.getStatusCode(), file.getName());
        }
    }

    /**
     * de http request om een file te verwijderen die gekopieerd staat op een andere node
     */
    public void deleteRequest(String fileName) {
        int fileHash = HashingFunction.getHashFromString(fileName);
        String nodeAddress = infoService.getNodes().get(nodesToStoreFilesOn.get(fileHash)).getSocketAddress();

        ResponseEntity<Object> response = restTemplate.exchange(
                "http://" + nodeAddress + "/files/replication/" + fileList.get(fileHash).getName(),
                HttpMethod.DELETE,
                null,
                Object.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            logger.debug("File deleted successfully: {}", fileName);
        } else {
            logger.debug("Failed to delete file. Status code: {}. File name: {}", response.getStatusCode(), fileName);
        }
    }

    public void clearPreviousLocalFiles() {
        previousLocalFiles = new HashSet<>();
        previousLocalFiles = new HashSet<>();
    }

    @Async
    @Scheduled(fixedRate = 5000)
    public void update() {
        // Updates list of new local files
        Set<String> currentLocalFiles = this.getFilesInPathAsSet(this.localFilesPath);
        Set<String> newFileSet = new HashSet<>(currentLocalFiles); // We need an unchanged file set for using it in next checks!
        newFileSet.removeAll(previousLocalFiles); // Removes

        for (String fileName : previousLocalFiles) {
            // Check if local file is still there
            if (!currentLocalFiles.contains(fileName)) {
                deleteRequest(fileName);
            }
        }

        // Update all local files
        previousLocalFiles = currentLocalFiles;
        if (newFileSet.isEmpty()) return;
        if (infoService.getNamingServerAddress() == null) {
            logger.warn("Naming server address is still not known. Skipping file replication");
            return;
        }

        // Create the file list
        fileList.clear();
        for (String fileName : newFileSet) {
            fileList.put(HashingFunction.getHashFromString(fileName), new File(this.localFilesPath, fileName));
        }

        // Send to naming server
        RestClient client = RestClient.builder()
                .baseUrl(infoService.getNamingServerAddress() + "/files/replication")
                .defaultHeader("Content-Type", "application/json")
                .build();
        nodesToStoreFilesOn = client.method(HttpMethod.POST).body(new ArrayList<>(fileList.keySet())).retrieve().body(new ParameterizedTypeReference<>() {});

        for (Integer hashFile : nodesToStoreFilesOn.keySet()) {
            int node = nodesToStoreFilesOn.get(hashFile);
            if (node == infoService.getSelfNode().hashCode()) {
                continue;
            }

            String nodeAddress = infoService.getNodes().get(node).getSocketAddress();
            File file = fileList.get(hashFile);
            transferRequest(file, nodeAddress);
        }
    }

    public void store(MultipartFile file) throws IOException {
        //TODO: adding file adds weird name to directory
        File targetFile = new File(this.replicatedFilesPath, file.getOriginalFilename());
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
        initialStream.close();
    }

    public void remove(String fileName) {
        // Remove file from file list
        Integer fileHash = HashingFunction.getHashFromString(fileName);
        fileList.remove(fileHash);
        // Remove file from directory
        File fileToRemove = new File(this.replicatedFilesPath, fileName);
        // Check if the file exists
        if (fileToRemove.exists()) {
            // Attempt to delete the file
            // Check if the file was successfully deleted
            if (fileToRemove.delete()) {
                System.out.println("File " + fileName + " has been successfully deleted.");
            } else {
                System.out.println("Failed to delete file " + fileName + ".");
            }
        } else {
            System.out.println("File " + fileName + " does not exist in the specified directory.");
        }
    }


    @PreDestroy
    public void destroy() {
        Set<String> localList = this.getFilesInPathAsSet(this.localFilesPath);
        Set<String> replicatedList = this.getFilesInPathAsSet(this.replicatedFilesPath);

        for (String fileName : localList) {
            deleteRequest(fileName);
        }

        for (String fileName : replicatedList) {
            File file = new File(this.replicatedFilesPath, fileName);

            // copy to this previous through a simple http request
            transferRequest(file, this.infoService.getNodes().get(this.infoService.getPreviousID()).getSocketAddress());

            //TODO: figure out how to check if the previous node of this one is the owner of the copied file
        }
    }

    private Set<String> getFilesInPathAsSet(String path) {
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptySet();
        }

        return Arrays.stream(dir.listFiles()).map(File::getName).collect(Collectors.toSet());
    }
}
