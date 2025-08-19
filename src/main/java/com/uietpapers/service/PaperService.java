package com.uietpapers.service;

import com.uietpapers.dto.PaperRequest;
import com.uietpapers.entity.Paper;
import com.uietpapers.entity.PendingPaper;
import com.uietpapers.repository.PaperRepository;
import com.uietpapers.repository.PendingPaperRepository;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.TessBaseAPI;


import static org.bytedeco.leptonica.global.leptonica.pixDestroy;
import static org.bytedeco.leptonica.global.leptonica.pixReadMem;
import static org.bytedeco.tesseract.global.tesseract.*;
 // This is the correct class



import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

@Service
public class PaperService {

    private final PaperRepository paperRepo;
    private final PendingPaperRepository pendingRepo;
    private final StorageService storage;

    private static final Logger logger = LoggerFactory.getLogger(PaperService.class);

    public PaperService(PaperRepository paperRepo,
                        PendingPaperRepository pendingRepo,
                        StorageService storage) {
        this.paperRepo = paperRepo;
        this.pendingRepo = pendingRepo;
        this.storage = storage;
    }

    // Upload + approve directly
    public Paper uploadAndApprovePaper(PaperRequest meta, MultipartFile file) throws Exception {
        PendingPaper pending = uploadPendingPaper(meta, file);

        // Extract text using JavaCPP Tesseract
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
        return lower.contains("exam") || lower.contains("sub");
    }

 private String extractTextFromPDF(MultipartFile file) throws IOException {
    StringBuilder extractedText = new StringBuilder();

    // 1️⃣ Copy tessdata from resources to a temporary folder
    ClassPathResource resource = new ClassPathResource("tessdata");
    File tempTessDataDir = Files.createTempDirectory("tessdata").toFile();

    File[] files = resource.getFile().listFiles();
    if (files != null) {
        for (File f : files) {
            Files.copy(f.toPath(), new File(tempTessDataDir, f.getName()).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // 2️⃣ Initialize Tesseract API
    try (TessBaseAPI api = new TessBaseAPI()) {
        // Use temp tessdata dir
        if (api.Init(tempTessDataDir.getAbsolutePath(), "eng") != 0) {
            throw new RuntimeException("Could not initialize Tesseract.");
        }

        // 3️⃣ Load PDF
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int pages = Math.min(3, document.getNumberOfPages());

            for (int page = 0; page < pages; page++) {
                BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(page, 200);

                // Convert BufferedImage to PIX (Leptonica)
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                byte[] imageBytes = baos.toByteArray();

                PIX pix = pixReadMem(imageBytes, imageBytes.length);
                if (pix == null) {
                    throw new IOException("Failed to convert BufferedImage to PIX.");
                }

                api.SetImage(pix);
                try (BytePointer outText = api.GetUTF8Text()) {
                    extractedText.append(outText.getString()).append("\n\n");
                }
                pixDestroy(pix); // clean up
            }
        }

        api.End(); // release Tesseract resources
    }

    String text = extractedText.toString();
    logger.info(text);
    return text;
}


    // OCR text extraction with JavaCPP Tesseract + PDFBox
    // private String extractTextFromPDF(MultipartFile file) throws IOException {
    //     StringBuilder extractedText = new StringBuilder();

    //     // 1️⃣ Load tessdata path
    //     // In Docker, tessdata is at /app/tessdata
    //     // Locally, you can use "src/main/resources/tessdata" if needed
    //     String tessDataPath = "/app/tessdata";

    //     try (TessBaseAPI api = new TessBaseAPI()) {
    //         if (api.Init(tessDataPath, "eng") != 0) {
    //             throw new RuntimeException("Could not initialize Tesseract.");
    //         }

    //         // 2️⃣ Load PDF
    //         try (PDDocument document = PDDocument.load(file.getInputStream())) {
    //             PDFRenderer pdfRenderer = new PDFRenderer(document);
    //             int pages = Math.min(3, document.getNumberOfPages());

    //             for (int page = 0; page < pages; page++) {
    //                 BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(page, 200);

    //                 // Convert BufferedImage to byte[] (PNG)
    //                 ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //                 ImageIO.write(bufferedImage, "png", baos);
    //                 baos.flush();
    //                 byte[] imageBytes = baos.toByteArray();
    //                 baos.close();

    //                 // Convert to PIX (Leptonica)
    //                 PIX pix = pixReadMem(imageBytes, imageBytes.length);
    //                 if (pix == null) {
    //                     throw new IOException("Failed to convert BufferedImage to PIX.");
    //                 }

    //                 api.SetImage(pix);
    //                 try (BytePointer outText = api.GetUTF8Text()) {
    //                     extractedText.append(outText.getString()).append("\n\n");
    //                 }

    //                 pixDestroy(pix); // cleanup
    //             }
    //         }
    //     }

    //     String text = extractedText.toString();
    //     logger.info(text);
    //     return text;
    // }




    // Search
    public List<Paper> search(String branch, String subject, Integer year, Integer semester, String examType) {
        return paperRepo.search(branch, subject, year, semester, examType);
    }

    // Delete paper + file
    public void deletePaper(UUID id) {
        Paper paper = paperRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Paper not found"));
        String fileUrl = paper.getFileUrl();
        String bucketPath = "/storage/v1/object/public/" + storage.getBucket() + "/";
        String filePath = null;

        if (fileUrl.contains(bucketPath)) {
            filePath = fileUrl.substring(fileUrl.indexOf(bucketPath) + bucketPath.length());
        }

        if (filePath != null) {
            try {
                storage.delete(filePath);
            } catch (Exception e) {
                logger.error("Failed to delete file from storage", e);
            }
        }

        paperRepo.delete(paper);
    }
}


//private String extractTextFromPDF(MultipartFile file) throws IOException {
//    StringBuilder extractedText = new StringBuilder();
//
//    // Path to tessdata inside resources
//    ClassPathResource resource = new ClassPathResource("tessdata");
//    File tessDataDir = resource.getFile();
//
//    try (TessBaseAPI api = new TessBaseAPI()) {
//        if (api.Init(tessDataDir.getAbsolutePath(), "eng") != 0) {
//            throw new RuntimeException("Could not initialize Tesseract.");
//        }
//
//        try (PDDocument document = PDDocument.load(file.getInputStream())) {
//            PDFRenderer pdfRenderer = new PDFRenderer(document);
//            int pages = Math.min(3, document.getNumberOfPages());
//
//            for (int page = 0; page < pages; page++) {
//                BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(page, 200);
//
//                // Convert BufferedImage to byte[] in PNG
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                ImageIO.write(bufferedImage, "png", baos);
//                baos.flush();
//                byte[] imageBytes = baos.toByteArray();
//                baos.close();
//
//                // Read as PIX (Leptonica)
//                PIX pix = pixReadMem(imageBytes, imageBytes.length);
//                if (pix == null) {
//                    throw new IOException("Failed to convert BufferedImage to PIX.");
//                }
//
//                api.SetImage(pix);
//                try (BytePointer outText = api.GetUTF8Text()) {
//                    extractedText.append(outText.getString()).append("\n\n");
//                }
//
//                pixDestroy(pix); // cleanup
//            }
//        }
//    }
//
//    String text = extractedText.toString();
//    logger.info(text);
//    return text;
//}


