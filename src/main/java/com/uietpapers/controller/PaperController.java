package com.uietpapers.controller;

import com.uietpapers.dto.PaperRequest;
import com.uietpapers.dto.UploadResponse;
import com.uietpapers.entity.Paper;
import com.uietpapers.entity.PendingPaper;
import com.uietpapers.service.AdminConfig;
import com.uietpapers.service.PaperService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api")
public class PaperController {

    private final PaperService service;

    public PaperController(PaperService service) {
        this.service = service;
    }

    // Upload -> pending -> validate -> approve automatically
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(
            @Validated @RequestPart("meta") PaperRequest meta,
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        Paper approvedPaper = null;
        try {
            approvedPaper = service.uploadAndApprovePaper(meta, file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(new UploadResponse(approvedPaper.getId(), approvedPaper.getFileUrl()));
    }


    @GetMapping("/papers")
    public List<Paper> search(
            @RequestParam(name = "branch", required = false) String branch,
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "semester", required = false) Integer semester,
            @RequestParam(name = "examType", required = false) String examType
    ) {
        return service.search(branch, subject, year, semester, examType);
    }

    // Delete paper (optional)
    @DeleteMapping("/admin/papers/{id}")
    public ResponseEntity<Void> deletePaper(@PathVariable UUID id) {
        service.deletePaper(id);
        return ResponseEntity.noContent().build();
    }
}

