package com.uietpapers.repository;

import com.uietpapers.entity.Paper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface PaperRepository extends JpaRepository<Paper, UUID> {

    @Query("""
    SELECT p FROM Paper p
    WHERE (:branch IS NULL OR :branch = '' OR LOWER(p.branch) = LOWER(:branch))
      AND (:subject IS NULL OR :subject = '' OR LOWER(p.subject) LIKE LOWER(CONCAT('%', :subject, '%')))
      AND (:year IS NULL OR p.year = :year)
      AND (:semester IS NULL OR p.semester = :semester)
      AND (:examType IS NULL OR :examType = '' OR UPPER(p.examType) = UPPER(:examType))
    ORDER BY p.uploadedAt DESC
""")
    List<Paper> search(
            @Param("branch") String branch,
            @Param("subject") String subject,
            @Param("year") Integer year,
            @Param("semester") Integer semester,
            @Param("examType") String examType
    );

}
