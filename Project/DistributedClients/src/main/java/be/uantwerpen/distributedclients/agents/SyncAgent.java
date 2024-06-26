package be.uantwerpen.distributedclients.agents;

import be.uantwerpen.distributedclients.services.FileService;
import be.uantwerpen.distributedclients.utils.HashingFunction;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class SyncAgent extends Agent {

    private final Logger logger = LoggerFactory.getLogger(SyncAgent.class);
    private FileService fileService;

    @Override
    protected void setup() {
        logger.info("SyncAgent started.");
        this.fileService = (FileService) getArguments()[0];
        addBehaviour(new FixDuplicateBehaviour(this, 1000, fileService));
    }

    public static class FixDuplicateBehaviour extends TickerBehaviour {

        private final FileService fileService;

        public FixDuplicateBehaviour(Agent a, long period, FileService fileService) {
            super(a, period);
            this.fileService = fileService;
        }

        @Override
        protected void onTick() {
            HashMap<Integer, File> localFiles = this.fileService.getLocalFileList();
            HashMap<Integer, File> replicatedFiles = this.fileService.getReplicatedFileList();

            List<String> localFileNames = localFiles.values().stream().map(File::getName).toList();
            List<String> replicatedFileNames = replicatedFiles.values().stream().map(File::getName).toList();

            for (String fileName : localFileNames) {
                if (replicatedFileNames.contains(fileName)) {
                    File file = new File(this.fileService.replicatedFilesDirectory, fileName);
                    replicatedFiles.remove(HashingFunction.getHashFromString(fileName));
                    if(file.exists()){
                        if(file.delete()){
                            System.out.println("Deleted file from replicated folder: " + fileName);
                        }
                    }
                }
            }
        }

    }
}
