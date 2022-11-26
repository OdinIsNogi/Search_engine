package searchengine.services;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.engine.Parser;
import searchengine.model.Index;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.dto.index.IndexResponse;
import searchengine.repository.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Service
@Getter
public class IndexServiceImpl implements IndexService {


    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private FieldRepository fieldRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private SitesList sitesList;

    boolean stopped;
    boolean isIndexing;
    private ExecutorService executorService;


    //Получает список сайтов и проверяет статус индексации
    @Override
    public IndexResponse startIndexing() throws SQLException {
        List<Site> sites = sitesList.getSites();
        if (isIndexing) {
            return new IndexResponse(true, "Индексация уже запущена");
        }
        isIndexing = true;
        for (Site s : sites) {
            if (s.getStatus() == Status.INDEXING) continue;
            prepareForIndexing(s);
        }
        return new IndexResponse(true);
    }

    private void prepareForIndexing(Site site) throws SQLException {
        Site siteToDelete = siteRepository.findByUrl(site.getUrl());
        executorService = Executors.newFixedThreadPool(sitesList.getSites().size());
        if (siteToDelete != null) {
            lemmaRepository.deleteAllBySiteId(siteToDelete.getId());
            pageRepository.deleteAllBySiteId(siteToDelete.getId());
            siteRepository.deleteByUrl(siteToDelete.getUrl());
        }

        executorService.execute(() -> {
            Site init = init(site, siteRepository);
            indexingThisSite(init);
        });
    }


    private Site init(Site site, SiteRepository repo) {
        site.setStatusTime(new Date());
        site.setStatus(Status.INDEXING);
        repo.save(site);
        return site;

    }

    public List<Index> indexingThisSite(Site site) {
        List<Index> indexes = null;
        try {
            Parser.setIsCanceled(false);
            Parser.setFields(fieldRepository.findAll());
            Parser parser = new Parser(site.getUrl());

            ForkJoinPool pool = ForkJoinPool.commonPool();
            pool.invoke(parser);

            indexes = parser.getIndexToDb();

//           isIndexing = false;


            for (int i = 0; i < indexes.size(); i++) {
                indexes.get(i).getPage().setSite(site);
                indexes.get(i).getLemma().setSite(site);
            }

            site.setStatusTime(new Date());
            if (!stopped) {
                site.setStatus(Status.INDEXED);
            }
            indexRepository.saveAll(indexes);
            LocalDateTime time = LocalDateTime.now();
            System.out.println(time);
        } catch (Exception e) {
            site.setStatus(Status.FAILED);
            site.setLastError(e.getMessage());
            e.printStackTrace();
        }

        siteRepository.save(site);
        return indexes;
    }


    public IndexResponse stopIndexing() {

        if (!isIndexing) {
            System.out.println("Не запущена");
            return new IndexResponse(false, "Индексация не запущена");
        }
        Parser.setIsCanceled(true);
        isIndexing = false;
        stopped = true;
        for (Site s : sitesList.getSites()) {
            s.setStatus(Status.FAILED);
            s.setLastError("Индексация остановлен пользователем");
            siteRepository.save(s);
        }
        return new IndexResponse(true);
    }


    public IndexResponse indexPage(String url) throws SQLException {
        List<Site> sites = sitesList.getSites();
        if (url.isBlank()) return new IndexResponse(false, "Задан пустой запрос");

        List<Site> sitesForIndex = sites.stream().filter(s -> s.getUrl().equals(url)).collect(Collectors.toList());

        for (Site site : sitesForIndex) {
            if (site.getStatus() == Status.INDEXING) {
                System.out.println("Уже запущена");
                return new IndexResponse(true, "Индексация уже запущена");
            } else {
                prepareForIndexing(site);
                return new IndexResponse(true);
            }

        }
        System.out.println("Левая ссылка");
        return new IndexResponse(false, "Данная страница находится за пределами сайтов," +
                "указанных в конфигурационном файле");
    }

    @Override
    public Optional<Index> getIndexById(long id) {
        return indexRepository.findById(id);
    }

    @Override
    public Iterable<Index> getIndexList() {
        return indexRepository.findAll();
    }

    @Override
    public void createIndex(Index index) {
        indexRepository.save(index);
    }

    @Override
    public void creatIndexSet(Collection<Index> indexes) {
        indexRepository.saveAll(indexes);
    }

    @Override
    public void deleteIndex(long id) {
        indexRepository.deleteById(id);
    }

    @Override
    public void deleteAllIndexes() {
        indexRepository.deleteAll();
    }

}
