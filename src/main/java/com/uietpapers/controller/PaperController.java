package com.uietpapers.controller;

import com.uietpapers.dto.PaperRequest;
import com.uietpapers.dto.UploadResponse;
import com.uietpapers.entity.Paper;
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

@RestController
@RequestMapping("/api")
public class PaperController {

    private final PaperService service;
    private final AdminConfig adminConfig;

    public PaperController(PaperService service, AdminConfig adminConfig) {
        this.service = service;
        this.adminConfig = adminConfig;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(
            @Validated @RequestPart("meta") PaperRequest meta,
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        Paper p = service.uploadPaper(meta, file);
        return ResponseEntity.ok(new UploadResponse(p.getId(), p.getFileUrl()));
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

    @DeleteMapping("/admin/papers/{id}")
    public ResponseEntity<Void> deletePaper(@PathVariable UUID id,
                                            @RequestHeader("X-ADMIN-KEY") String adminKey) {
        if (!adminConfig.getAdminKey().equals(adminKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }


        try {
            service.deletePaper(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }



}
