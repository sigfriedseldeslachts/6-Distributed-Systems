package be.uantwerpen.distributedclients.controllers;

import be.uantwerpen.distributedclients.services.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    // dit is wat de receiving node moet doen
    @PostMapping("/replication")
    public ResponseEntity<Object> handleFileUpload(@RequestParam("file") MultipartFile file) throws IOException {
        fileService.store(file);
        return ResponseEntity.noContent().build();
    }

}
