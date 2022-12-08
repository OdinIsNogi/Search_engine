package searchengine.repository;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;

import java.util.List;

@Repository
public interface SiteRepository extends CrudRepository<Site, Long> {
    @Transactional
    void deleteByUrl(String name);

    Site findByUrl(String url);

    @Transactional
    @Query(value = "SELECT * FROM site", nativeQuery = true)
    List<Site> findAll();
}
