package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.engine.Parser;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final IndexServiceImpl indexService;
    @Autowired
    private SitesList sites;


    @Override
    public StatisticsResponse getStatistics() {
        List<Site> sitesList = indexService.getSitesList().getSites();

        TotalStatistics total = new TotalStatistics();
        total.setSites(sitesList.size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        int pages;
        int lemmas;
        for (Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            if (site.getStatus() == Status.INDEXING) {
                pages = site.getParser().countPages();
                lemmas = site.getParser().countLemmas();
            } else {
                pages = pageRepository.countAll(site.getId());
                lemmas = lemmaRepository.countAll(site.getId());
            }
            Site temp = siteRepository.findByUrl(site.getUrl());
            String error;
            String status;
            if (temp == null) {
                status = "";
                error = "";
            } else {
                status = temp.getStatus().toString();
                error = temp.getLastError();
            }

            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(status);
            item.setError(error);
            item.setStatusTime(System.currentTimeMillis());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
