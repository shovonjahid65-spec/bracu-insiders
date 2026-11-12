package bd.bracuinsiders.repository;

import bd.bracuinsiders.model.Letter;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LetterRepository extends JpaRepository<Letter, Long> {
    Page<Letter> findByArchivedOrderByCreatedAtDescIdDesc(boolean archived, Pageable page);

    Optional<Letter> findByReceiptHash(String receiptHash);

    long countByArchivedFalseAndReadFalse();
}
