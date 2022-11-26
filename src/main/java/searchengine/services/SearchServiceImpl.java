package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchResponse;
import searchengine.engine.Searcher;
import searchengine.model.RelevantPage;
import searchengine.model.Site;
import searchengine.repository.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private FieldRepository fieldRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private SitesList sitesList;
    private String query;
    private List<RelevantPage> relevantPages;


    @Override
    public SearchResponse search(String query, String site, int offset, int limit) throws IOException {
        relevantPages = new ArrayList<>();
        List<Site> sites = sitesList.getSites();

        if (query.isBlank()) {
            return new SearchResponse(false, "Задан пустой поисковой запрос");
        }

        if (site == null) {
            for (Site s : sites) {
                relevantPages.addAll(new Searcher(query, lemmaRepository, s.getId()).getFoundPages());
            }
        } else {
            Site siteForSearch = siteRepository.findByUrl(site);
            if (siteForSearch == null) return new SearchResponse(false, "Указанная страница не найдена");
            relevantPages.addAll(new Searcher(query, lemmaRepository, siteForSearch.getId()).getFoundPages());
        }
        return new SearchResponse(true, relevantPages.size(), relevantPages.subList(offset, Math.min(relevantPages.size(), offset + limit)));
    }
}
