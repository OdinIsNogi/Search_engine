package searchengine.repository;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;

@Repository
public interface SiteRepository extends CrudRepository<Site, Long> {
    @Transactional
    void deleteByUrl(String name);
//    @Query(value = "SELECT IFNULL(site.s)", nativeQuery = true)
    Site findByUrl(String url);
}
