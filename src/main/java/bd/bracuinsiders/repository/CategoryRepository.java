package bd.bracuinsiders.repository;

import bd.bracuinsiders.model.Category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByIdAsc();

    Optional<Category> findBySlug(String slug);

    boolean existsByNameIgnoreCaseAndIdNot(String name, long id);
}
