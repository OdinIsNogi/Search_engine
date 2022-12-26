package searchengine.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexServiceImpl;
import searchengine.dto.index.IndexResponse;

import java.sql.SQLException;

@RestController()
public class IndexController {
    private final IndexServiceImpl service;
    private final SiteRepository siteRepository;

    public IndexController(IndexServiceImpl service, SiteRepository siteRepository) {
        this.service = service;
        this.siteRepository = siteRepository;
    }

    @GetMapping("/api/startIndexing")
    public IndexResponse startIndexing() throws SQLException {
        if (service.isIndexing()) {
            return new IndexResponse(true, "Индексация уже запущена");
        }
        return service.startIndexing();
    }

    @PostMapping("/api/indexPage")
    public IndexResponse indexPage(@RequestParam String url) throws SQLException {
        Site site = siteRepository.findByUrl(url);
        if (site == null) return new IndexResponse(false, "Данная страница находится за пределами сайтов," +
                "указанных в конфигурационном файле");
        if (url.isBlank()) return new IndexResponse(false, "Задан пустой запрос");
        if (site.getStatus() == Status.INDEXING) return new IndexResponse(true, "Индексация уже запущена");
        return service.indexPage(url);
    }

    @GetMapping("api/stopIndexing")
    private IndexResponse stopIndexing() {
        if (!service.isIndexing()) {
            return new IndexResponse(false, "Индексация не запущена");
        }
        return service.stopIndexing();
    }

}
