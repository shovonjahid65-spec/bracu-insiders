package bd.bracuinsiders.repository;

import bd.bracuinsiders.model.Post;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;

import java.util.*;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    Optional<Post> findBySlugAndPublishedTrue(String slug);

    Optional<Post> findFirstByPublishedTrueOrderByFeaturedDescPublishedAtDescIdDesc();

    long countByPublishedTrue();

    long countByCategoryId(long categoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    Optional<Post> forUpdate(long id);

    @Query(
            value =
                    """
                    SELECT p.* FROM posts p JOIN post_reads r ON r.post_id=p.id
                    WHERE p.published ORDER BY r.views DESC, p.published_at DESC, p.id DESC LIMIT 4
                    """,
            nativeQuery = true)
    List<Post> popular();
}
