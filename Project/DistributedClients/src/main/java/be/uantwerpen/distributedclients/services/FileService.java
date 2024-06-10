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
    private final HashMap<Integer, File> replicatedFileList = new HashMap<>();
    private Map<Integer, Integer> nodesToStoreFilesOn = new HashMap<>();
    private final RestTemplate restTemplate;
    private final File replicatedFilesDirectory;
    private final File localFilesDirectory;
    private Set<String> previousLocalFiles = new HashSet<>();

    private final HashMap<Integer, Set<Integer>> replicatedFilesToNodes = new HashMap<>(); // Keeps PER file a set of node hashes

    public FileService(InfoService infoService, Environment env, RestClient.Builder restClientBuilder) {
        String defaultDirectory = env.getProperty("app.directory", "");
        if (defaultDirectory.isEmpty()) {
            throw new IllegalArgumentException("app.directory is not set in application.properties");
        }

        this.infoService = infoService;
        this.replicatedFilesDirectory = new File(defaultDirectory, "replicated");
        this.localFilesDirectory = new File(defaultDirectory, "local");
        this.restTemplate = new RestTemplateBuilder().build();

        // Create directories
        this.createDirectory(replicatedFilesDirectory);
        this.createDirectory(localFilesDirectory);
    }

    /**
     * de http request om file te transferen en kopieren op andere node
     */
    public void transferRequest(File file, String nodeAddress, boolean shouldStoreLocally) {
        logger.info("Transferring file: {} to node: {}", file.getName(), nodeAddress);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file.getAbsolutePath()));
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> responseEntity =  restTemplate.exchange(
                "http://" + nodeAddress + "/files/replication?isLocal=" + shouldStoreLocally,
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

    @Async
    @Scheduled(fixedRate = 5000)
    public void update() {
        // Updates list of new local files
        Set<String> currentLocalFiles = this.getFilesInDirAsSet(this.localFilesDirectory);
        for (String fileName : currentLocalFiles) {
            fileList.put(HashingFunction.getHashFromString(fileName), new File(this.localFilesDirectory, fileName));
        }

        // Update the replicated file list. NOTE: At this moment it serves no purpose other than being used in the GUI.
        replicatedFileList.clear();
        for (String fileName : this.getFilesInDirAsSet(this.replicatedFilesDirectory)) {
            replicatedFileList.put(HashingFunction.getHashFromString(fileName), new File(this.replicatedFilesDirectory, fileName));
        }

        // Check if any files need to be deleted
        for (String fileName : previousLocalFiles) {
            // Check if local file is still there
            if (!currentLocalFiles.contains(fileName)) {
                deleteRequest(fileName);
            }
        }
        previousLocalFiles = currentLocalFiles; // Now that we have checked if files need to be deleted, we can update the previous local files
        if (infoService.getNamingServerAddress() == null) {
            logger.warn("Naming server address is still not known. Skipping file replication");
            return;
        }

        // Send to naming server
        RestClient client = RestClient.builder()
                .baseUrl(infoService.getNamingServerAddress() + "/files/replication")
                .defaultHeader("Content-Type", "application/json")
                .build();
        nodesToStoreFilesOn = client.method(HttpMethod.POST).body(new ArrayList<>(fileList.keySet())).retrieve().body(new ParameterizedTypeReference<>() {});

        // Go over each files
        for (Integer hashFile : nodesToStoreFilesOn.keySet()) {
            int replicationNode = nodesToStoreFilesOn.get(hashFile); // Get the node that we should replicate to according to the naming server
            if (replicationNode == infoService.getSelfNode().hashCode()) {
                replicationNode = infoService.getNextID();
            }

            Set<Integer> nodesThatWeReplicatedAFileTo = replicatedFilesToNodes.getOrDefault(hashFile, new HashSet<>()); // Keeps track of which file has been replicated to which node

            // If the replication node is already in the Set, we already replicated it and do nothing
            if (nodesThatWeReplicatedAFileTo.contains(replicationNode)) continue;

            // Otherwise we should replicate
            transferRequest(
                    fileList.get(hashFile),
                    infoService.getNodes().get(replicationNode).getSocketAddress(),
                    false
            );

            nodesThatWeReplicatedAFileTo.add(replicationNode); // Add the replication node to it

            replicatedFilesToNodes.put(hashFile, nodesThatWeReplicatedAFileTo);
        }
    }

    public void store(MultipartFile file, boolean isLocal) throws IOException {
        File targetFile = new File(isLocal ? this.localFilesDirectory : this.replicatedFilesDirectory, file.getOriginalFilename());
        logger.info("Storing file: {}, Locally?: {}", targetFile.getAbsoluteFile(), isLocal);

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

        // Check if we already had this file in the replicated folder
        if (isLocal) {
            File replicatedFilePath = new File(this.replicatedFilesDirectory, file.getOriginalFilename());
            if (!replicatedFilePath.exists()) { // If the file does not exist, skip
                return;
            }

            try {
                replicatedFilePath.delete();
            } catch (Exception e) {
                logger.error("Failed to delete replicated file after receiving ownership: {}", e.getMessage());
            }
        }
    }

    public void remove(String fileName) {
        // Remove file from file list
        Integer fileHash = HashingFunction.getHashFromString(fileName);
        fileList.remove(fileHash);
        // Remove file from directory
        File fileToRemove = new File(this.replicatedFilesDirectory, fileName);
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
        Set<String> localList = this.getFilesInDirAsSet(this.localFilesDirectory);
        Set<String> replicatedList = this.getFilesInDirAsSet(this.replicatedFilesDirectory);

        // Do nothing if we are the only node
        if (this.infoService.getNodes().size() <= 1) {
            return;
        }

        // When node stops, its own files should be transferred to the previous node
        for (String fileName : localList) {
            File file = new File(this.localFilesDirectory, fileName);
            transferRequest(file, this.infoService.getNodes().get(this.infoService.getPreviousID()).getSocketAddress(), true);
            try {
                file.delete();
            } catch (Exception e) {
                logger.error("Failed to delete file after transfer ship: {}", e.getMessage());
            }
        }

        for (String fileName : replicatedList) {
            File file = new File(this.replicatedFilesDirectory, fileName);
            transferRequest(file, this.infoService.getNodes().get(this.infoService.getPreviousID()).getSocketAddress(), false);
        }
    }

    /**
     *
     * @param dir
     * @return A set of files in the directory
     */
    private Set<String> getFilesInDirAsSet(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptySet();
        }

        return Arrays.stream(dir.listFiles()).map(File::getName).collect(Collectors.toSet());
    }

    private boolean createDirectory(File path) {
        if (!path.exists()) {
            boolean success = path.mkdirs();

            if (success) {
                logger.info("Created directory: {}", path);
            } else {
                logger.error("Failed to create directory: {}", path);
            }

            return success;
        }

        return true;
    }

    public HashMap<Integer, File> getLocalFileList() {
        return fileList;
    }

    public HashMap<Integer, File> getReplicatedFileList() {
        return replicatedFileList;
    }

    public boolean hasLocalFile(String originalFilename) {
        return this.previousLocalFiles.contains(originalFilename);
    }
}
