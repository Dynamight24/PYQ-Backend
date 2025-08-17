package com.uietpapers.service;

import com.uietpapers.dto.PaperRequest;
import com.uietpapers.entity.Paper;
import com.uietpapers.entity.PendingPaper;
import com.uietpapers.repository.PaperRepository;
import com.uietpapers.repository.PendingPaperRepository;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PaperService {

    private final PaperRepository paperRepo;
    private final PendingPaperRepository pendingRepo;
    private final StorageService storage;

    private static final Logger logger = LoggerFactory.getLogger(PaperService.class);

    public PaperService(PaperRepository paperRepo, PendingPaperRepository pendingRepo, StorageService storage) {
        this.paperRepo = paperRepo;
        this.pendingRepo = pendingRepo;
        this.storage = storage;
    }

    // Upload + approve directly
    public Paper uploadAndApprovePaper(PaperRequest meta, MultipartFile file) throws Exception {
        PendingPaper pending = uploadPendingPaper(meta, file);

        // Extract text using OCR
        String text = extractTextFromPDF(file);

        if (!validatePaperText(text)) {
            throw new RuntimeException("PDF does not seem like a valid exam paper");
        }

        Paper paper = new Paper();
        paper.setTitle(meta.title());
        paper.setBranch(meta.branch());
        paper.setSubject(meta.subject());
        paper.setYear(meta.year());
        paper.setSemester(meta.semester());
        paper.setExamType(meta.examType());
        paper.setFileUrl(pending.getFileUrl());

        Paper saved = paperRepo.save(paper);
        pendingRepo.delete(pending);

        return saved;
    }

    // Save to pending
    public PendingPaper uploadPendingPaper(PaperRequest meta, MultipartFile file) throws Exception {
        String filename = StorageService.safeFilename(file.getOriginalFilename());
        String path = String.format("%s/%d/sem-%d/%s/%s",
                meta.branch().toLowerCase(),
                meta.year(),
                meta.semester(),
                meta.examType().toUpperCase(),
                filename);

        String url = storage.upload(path, file.getBytes(), file.getContentType());

        PendingPaper pending = new PendingPaper();
        pending.setTitle(meta.title());
        pending.setBranch(meta.branch());
        pending.setSubject(meta.subject());
        pending.setYear(meta.year());
        pending.setSemester(meta.semester());
        pending.setExamType(meta.examType());
        pending.setFileUrl(url);
        pending.setApproved(false);

        return pendingRepo.save(pending);
    }

    // Validate text
    private boolean validatePaperText(String text) {
        String lower = text.toLowerCase();
        return lower.contains("exam") || lower.contains("question") || lower.contains("marks");
    }

    // OCR text extraction only (no PDFBox)
    private String extractTextFromPDF(MultipartFile file) throws IOException {
    // Step 1: Copy tessdata to a temporary folder
    File tessDataTemp = new File(System.getProperty("java.io.tmpdir"), "tessdata");
    if (!tessDataTemp.exists()) {
        tessDataTemp.mkdirs();

        Path resourceTess = Paths.get("src/main/resources/tessdata");
        Files.walk(resourceTess).forEach(path -> {
            try {
                Path dest = tessDataTemp.toPath().resolve(resourceTess.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // Step 2: Save the uploaded PDF temporarily
    File tempFile = File.createTempFile("upload-", ".pdf");
    file.transferTo(tempFile);

    // Step 3: Run OCR
    Tesseract tesseract = new Tesseract();
    tesseract.setDatapath(tessDataTemp.getAbsolutePath());
    tesseract.setLanguage("eng");

    String extractedText = "";
    try {
        extractedText = tesseract.doOCR(tempFile);
    } catch (TesseractException e) {
        logger.error("OCR failed: ", e);
    } finally {
        tempFile.delete();
    }

    logger.info("OCR extraction completed, length={}", extractedText.length());
    logger.debug("Full OCR text: {}", extractedText);

    return extractedText;
}


    // Search
    public List<Paper> search(String branch, String subject, Integer year, Integer semester, String examType) {
        return paperRepo.search(branch, subject, year, semester, examType);
    }

    // Delete paper + file
    public void deletePaper(UUID id) {
        Paper paper = paperRepo.findById(id).orElseThrow(() -> new RuntimeException("Paper not found"));
        String fileUrl = paper.getFileUrl();
        String bucketPath = "/storage/v1/object/public/" + storage.getBucket() + "/";
        String filePath = null;

        if (fileUrl.contains(bucketPath)) {
            filePath = fileUrl.substring(fileUrl.indexOf(bucketPath) + bucketPath.length());
        }

        if (filePath != null) {
            try { storage.delete(filePath); } catch (Exception e) { e.printStackTrace(); }
        }

        paperRepo.delete(paper);
    }
}
