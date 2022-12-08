package searchengine.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface PageRepository extends CrudRepository<Page, Long> {
    List<Page> findByPath(String path);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM pages where pages.site_id=:id", nativeQuery = true)
    void deleteAllBySiteId(@Param("id") long id);

    @Query(value = "SELECT COUNT(*) from pages where pages.site_id=:id", nativeQuery = true)
    int countAll(@Param("id") long id);
}
