package be.uantwerpen.distributedclients.services;

import be.uantwerpen.distributedclients.utils.HashingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Service
public class SyncAgent implements Runnable, Serializable {
    private final Logger logger = LoggerFactory.getLogger(SyncAgent.class);
    private final FileService fileService;
    private final InfoService infoService;

    public SyncAgent(FileService fileService, InfoService infoService) {
        this.fileService = fileService;
        this.infoService = infoService;
    }

    @Override
    public void run() {
        logger.info("SyncAgent started.");

        this.checkDuplicates();

        this.locking();
    }

    @Async
    @Scheduled(fixedRate = 5000)
    public void checkDuplicates() {
        // if a file is both in replicated and local, the one in replicated has to be deleted
        HashMap<Integer, File> localFiles = this.fileService.getLocalFileList();
        HashMap<Integer, File> replicatedFiles = this.fileService.getReplicatedFileList();

        List<String> localFileNames = localFiles.values().stream().map(File::getName).toList();
        List<String> replicatedFileNames = replicatedFiles.values().stream().map(File::getName).toList();

        for (String fileName : localFileNames) {
            File file = new File(this.fileService.replicatedFilesDirectory, fileName);
            if (replicatedFileNames.contains(fileName)) {
                replicatedFiles.remove(HashingFunction.getHashFromString(fileName));
                if(file.exists() && file.delete()){
                    System.out.println("Deleted file from replicated folder: " + fileName);
                }
            }
        }
    }

    @Async
    @Scheduled(fixedRate = 5000)
    public void locking() {
        // check if a local file is being opened
        for (File file : this.fileService.getLocalFileList().values()) {
            if (file.canWrite()) {
                // TODO: tell everyone to lock the file
                Set<Integer> toBeNotifiedNodesHashes = this.fileService.getLocalFilesAndNodesWhereCopyIs().get(file.getName().hashCode());
                for (int nodeHash : toBeNotifiedNodesHashes) {
                    this.fileService.lockRequest(file.getName(), this.infoService.getNodes().get(nodeHash).getSocketAddress(), true);
                }
            }
        }

        // check if a replicated file is being opened
        for (File file : this.fileService.getReplicatedFileList().values()) {
            if (file.canWrite()) {
                //TODO: tell the node where it's stored locally to lock the file
                int localNodeToBeNotifiedHash = this.fileService.getReplicatedFilesAndLocalNodeList().get(file.getName().hashCode());
                this.fileService.lockRequest(file.getName(), this.infoService.getNodes().get(localNodeToBeNotifiedHash).getSocketAddress(), true);
                //TODO: and warn the others nodes that have a copy of the file
            }
        }
    }
}
