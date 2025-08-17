package com.uietpapers.repository;

import com.uietpapers.entity.PendingPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PendingPaperRepository extends JpaRepository<PendingPaper, UUID> {
}

