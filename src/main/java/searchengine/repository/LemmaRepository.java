package searchengine.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestParam;
import searchengine.model.Lemma;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface LemmaRepository extends CrudRepository<Lemma, Long> {
    @Query(value = "SELECT * FROM lemma where lemma.lemma IN :searched_lemma " +
            "and lemma.frequency < 100 " +
            "and lemma.site_id=:id order by lemma.frequency", nativeQuery = true)
    List<Lemma> findLemmaByName(@Param("id") long id, @Param("searched_lemma") String... lemma);

    List<Lemma> findAll();

    @Query(value = "SELECT count(*) from lemma",nativeQuery = true)
    int countAll();

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM lemma where lemma.site_id=:id", nativeQuery = true)
    void deleteAllBySiteId(@Param("id") long id);
}
