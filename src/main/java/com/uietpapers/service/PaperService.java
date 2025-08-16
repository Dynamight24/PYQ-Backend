package com.uietpapers.service;

import com.uietpapers.dto.PaperRequest;
import com.uietpapers.entity.Paper;
import com.uietpapers.repository.PaperRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@Service
public class PaperService {

    private final PaperRepository repo;
    private final StorageService storage;

    public PaperService(PaperRepository repo, StorageService storage) {
        this.repo = repo;
        this.storage = storage;
    }

    public Paper uploadPaper(PaperRequest meta, MultipartFile file) throws Exception {
        String filename = StorageService.safeFilename(file.getOriginalFilename());
        String path = String.format("%s/%d/sem-%d/%s/%s",
                meta.branch().toLowerCase(),
                meta.year(),
                meta.semester(),
                meta.examType().toUpperCase(),
                filename);

        String url = storage.upload(path, file.getBytes(), file.getContentType());

        Paper p = new Paper();
        p.setTitle(meta.title());
        p.setBranch(meta.branch());
        p.setSubject(meta.subject());
        p.setYear(meta.year());
        p.setSemester(meta.semester());
        p.setExamType(meta.examType());
        p.setFileUrl(url);

        return repo.save(p);
    }

    public List<Paper> search(String branch, String subject, Integer year, Integer semester, String examType) {
        return repo.search(branch, subject, year, semester, examType);
    }
    public void deletePaper(UUID id) {
        Paper paper = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Paper not found"));

        // Extract file path relative to bucket safely
        String fileUrl = paper.getFileUrl();
        String bucketPath = "/storage/v1/object/public/" + storage.getBucket() + "/";
        String filePath = null;

        if (fileUrl.contains(bucketPath)) {
            filePath = fileUrl.substring(fileUrl.indexOf(bucketPath) + bucketPath.length());
        } else {
            System.out.println("Warning: Unexpected fileUrl format, skipping Supabase deletion: " + fileUrl);
        }

        // Delete from Supabase bucket if path is valid
        if (filePath != null) {
            try {
                storage.delete(filePath); // this is the safe delete version we updated
            } catch (Exception e) {
                // Log the error but continue to delete from DB
                e.printStackTrace();
                System.out.println("Failed to delete file from Supabase: " + filePath);
            }
        }

        // Delete from DB
        repo.delete(paper);
    }




}
