package be.uantwerpen.distributedclients.controllers;

import be.uantwerpen.distributedclients.services.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/list")
    public HashMap<Integer, File> getFiles(@RequestParam(value = "replicated", required = false, defaultValue = "false") boolean replicated) {
        if (replicated) {
            return this.fileService.getReplicatedFileList();
        }

        return this.fileService.getLocalFileList();
    }

    // dit is wat de receiving node moet doen
    @PostMapping("/replication")
    public ResponseEntity<Object> handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("isLocal") boolean isLocal) throws IOException {
        fileService.store(file, isLocal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/replication/{filename}")
    public ResponseEntity<Object> removeFile(@PathVariable String filename) {
        fileService.remove(filename);
        return ResponseEntity.noContent().build();
    }
}
