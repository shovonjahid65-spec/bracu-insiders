package bd.bracuinsiders.repository;

import bd.bracuinsiders.model.Reply;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReplyRepository extends JpaRepository<Reply, Long> {
    List<Reply> findByMessageIdOrderBySentAtAscIdAsc(long messageId);
}
