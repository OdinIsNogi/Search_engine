package searchengine.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.services.IndexServiceImpl;
import searchengine.dto.index.IndexResponse;
import searchengine.services.IndexService;

import java.sql.SQLException;

@RestController
public class IndexController {
    private final IndexServiceImpl service;

    public IndexController(IndexServiceImpl service) {
        this.service = service;
    }

    @GetMapping("/startIndexing")
    public IndexResponse startIndexing() throws SQLException {
       return service.startIndexing();
    }

    @PostMapping("/indexPage")
    public IndexResponse indexPage(@RequestParam String url) throws SQLException {
        return service.indexPage(url);
    }

}
