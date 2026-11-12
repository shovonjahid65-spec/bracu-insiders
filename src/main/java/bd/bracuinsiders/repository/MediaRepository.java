package bd.bracuinsiders.repository;

import bd.bracuinsiders.model.Media;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;

import java.util.Optional;

public interface MediaRepository extends JpaRepository<Media, Long> {
    @Query("select distinct m from Post p join p.images m where p.published=true and m.id=:id")
    Optional<Media> published(long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Media m where m.id=:id")
    Optional<Media> forUpdate(long id);

    @Query(value = "select count(*) from post_media where media_id=:id", nativeQuery = true)
    long uses(long id);
}
