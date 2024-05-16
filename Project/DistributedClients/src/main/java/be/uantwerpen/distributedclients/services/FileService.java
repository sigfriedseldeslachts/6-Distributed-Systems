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
    private File[] previousLocalFiles;

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
    public ResponseEntity<String> transferRequest(File file, String nodeAddress) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(
                "http://" + nodeAddress + "/files/replication",
                HttpMethod.POST,
                requestEntity,
                String.class
        );
    }

    /**
     * de http request om een file te verwijderen die gekopieerd staat op een andere node
     */
    public ResponseEntity<String> deleteRequest(int fileHash, String nodeAddress) {
        return restTemplate.exchange(
                "http://" + nodeAddress + "/files/replication/" + fileList.get(fileHash).getName(),
                HttpMethod.DELETE,
                null,
                String.class
        );
    }

    /**
     * dit is de node die zijn request stuurt naar de receiving node
     */
    public void send() {
        if (infoService.getNamingServerAddress() == null) {
            logger.warn("Naming server address is still not known. Skipping file replication");
            return;
        }

        // send a request to namingserver to get info on who to connect to
        RestClient client = RestClient.builder()
                .baseUrl(infoService.getNamingServerAddress() + "/files/replication")
                .defaultHeader("Content-Type", "application/json")
                .build();
        nodesToStoreFilesOn = client.method(HttpMethod.POST).body(new ArrayList<>(fileList.keySet())).retrieve().body(new ParameterizedTypeReference<>() {});

        // send a request to connecting node
        for (Integer hashFile : nodesToStoreFilesOn.keySet()) {
            int node = nodesToStoreFilesOn.get(hashFile);
            if (node == infoService.getSelfNode().hashCode()) {
                continue;
            }

            String nodeAddress = infoService.getNodes().get(node).getSocketAddress();
            File file = fileList.get(hashFile);
            ResponseEntity<String> responseEntity = transferRequest(file, nodeAddress);

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                logger.debug("File uploaded successfully: {}", file.getName());
            } else {
                logger.debug("Failed to upload file. Status code: {}. File name: {}", responseEntity.getStatusCode(), file.getName());
            }
        }

    }

    @Async
    @Scheduled(fixedRate = 5000)
    public void update() {
        /*
        File replicatedFilesDirectory = new File(this.replicatedFilesPath);
        File[] replicatedFiles = replicatedFilesDirectory.listFiles();
        if (replicatedFiles != null) {
            for (File file : replicatedFiles) {
                fileList.put(HashingFunction.getHashFromString(file.getName()), file);
            }
        }*/

        // Updates list of new local files
        File localFilesDirectory = new File(this.localFilesPath);
        File[] currentLocalFiles = localFilesDirectory.listFiles();
        HashSet<File> currentLocalFilesSet = new HashSet<>(Arrays.asList(currentLocalFiles));
        HashSet<File> previousLocalFilesSet = new HashSet<>(Arrays.asList(previousLocalFiles));
        if (currentLocalFiles != null) {
            for (File currentFile : currentLocalFiles) {
                // Check if local file is still there
                if (!previousLocalFilesSet.contains(currentFile)) {
                    fileList.put(HashingFunction.getHashFromString(currentFile.getName()), currentFile);
                }
            }
        }

        // updates list for deleted files
        if (previousLocalFiles != null) {
            for (File file : previousLocalFiles) {
                // Check if local file is still there
                if (!currentLocalFilesSet.contains(file)) {
                    // Remove file on other nodes
                    // Get filehash
                    int fileHash = HashingFunction.getHashFromString(file.getName());
                    // Get node its saved on
                    String nodeAddress = infoService.getNodes().get(nodesToStoreFilesOn.get(fileHash)).getSocketAddress();
                    // Do delete request
                    ResponseEntity<String> responseEntity = deleteRequest(fileHash, nodeAddress);

                    if (responseEntity.getStatusCode() == HttpStatus.OK) {
                        logger.debug("File deleted successfully: {}", file.getName());
                    } else {
                        logger.debug("Failed to delete file. Status code: {}. File name: {}", responseEntity.getStatusCode(), file.getName());
                    }
                }
            }
        }

        // Update all local files
        previousLocalFiles = localFilesDirectory.listFiles();
    }

    public void store(MultipartFile file) throws IOException {
        File targetFile = new File(this.replicatedFilesPath, Objects.requireNonNull(file.getOriginalFilename()));
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
        File fileToRemove = new File(this.replicatedFilesPath, fileName);
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


    @PreDestroy
    public void destroy() {
        List<File> localList = Arrays.asList(previousLocalFiles);

        for (File file : fileList.values()) {
            // if local file -> tell copied node to destroy copies
            if (localList.contains(file)) {
                int fileHash = HashingFunction.getHashFromString(file.getName());
                ResponseEntity<String> responseEntity = deleteRequest(fileHash, infoService.getNodes().get(nodesToStoreFilesOn.get(fileHash)).getSocketAddress());
                if (responseEntity.getStatusCode() == HttpStatus.OK) {
                    logger.debug("File deleted successfully: {}", file.getName());
                } else {
                    logger.debug("Failed to delete file. Status code: {}. File name: {}", responseEntity.getStatusCode(), file.getName());
                }
                return;
            }
            // copy to this previous through a simple http request
            transferRequest(file, this.infoService.getNodes().get(this.infoService.getPreviousID()).getSocketAddress());

            //TODO: figure out how to check if the previous node of this one is the owner of the copied file
        }
    }
}
