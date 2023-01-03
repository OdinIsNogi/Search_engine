package searchengine.engine;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.springframework.data.util.Pair;
import searchengine.model.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Обходим страницы сайта и заполняем сущности для передачи в Базу данных.
 */


@Slf4j
public class Parser extends RecursiveAction {

    private final String url;
    private final String root;
    private static boolean isCanceled;

    private final Map<String, Page> pagesToDb;
    private final Map<String, Lemma> lemmaToDb;
    private final Map<Pair<String, String>, Index> indexToDb;

    private static Iterable<Field> fields;

    public static void setFields(Iterable<Field> fields) {
        Parser.fields = fields;
    }

    public Parser(String url) {
        this.url = url;
        this.root = url;

        pagesToDb = new ConcurrentHashMap<>();
        lemmaToDb = new ConcurrentHashMap<>();
        indexToDb = new ConcurrentHashMap<>();
    }


    public Parser
            (String url,
             String root,
             Map<Pair<String, String>, Index> indexMap,
             Map<String, Page> pagesToDb,
             Map<String, Lemma> lemmaToDb) {
        this.url = url;
        this.root = root;
        this.indexToDb = indexMap;
        this.pagesToDb = pagesToDb;
        this.lemmaToDb = lemmaToDb;
    }


    @SneakyThrows
    @Override
    public void compute() {
        if (isCanceled) return;
        try {
//            log.info(url);
            List<Parser> tasks = new ArrayList<>();
            Connection connection = Jsoup.connect(url).timeout(20_000)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com");

            Page curPage = addPage(connection);
            if (curPage == null) return;

            Document doc = connection.get();

            Map<Lemma, Integer> curLemmas = addLemmas(doc);
            Set<Index> indexes = addIndex(curPage, curLemmas);
            curPage.setIndexes(indexes);
            curLemmas.forEach((lemma, amount) -> lemma.setIndexes(indexes));

            Elements elements = doc.select("a");

            for (Element e : elements) {
                String child = e.absUrl("href");
                if (isCorrect(child)) {
                    Parser parser = new Parser(child, root, indexToDb, pagesToDb, lemmaToDb);
                    tasks.add(parser);
                }
            }
            ForkJoinTask.invokeAll(tasks);
        } catch (IOException e) {
            log.error("ERROR during parsing" + url);
        }
    }

    private String shortLink(String url) {
        log.info(url);
        url = url.replace(root, "");
//        url = url.toLowerCase(Locale.ROOT);
//        String regex = ".+\\w/";
//        if (url.matches(regex)) {
//            url = url.substring(0, url.length() - 1);
//            return url;
//        }
        return url;
    }

    public boolean isCorrect(String child) {
        return child.contains(root)
                && !pagesToDb.containsKey(shortLink(child))
                && !child.contains("#")
                && !child.contains("?")
                && !child.matches(".+(.jpg|.png|.pdf)$")
                && !child.matches(".+(.jpg/|.png/|.pdf/)$");
    }

    //добавляем страницы
    private Page addPage(Connection connection) throws IOException {
        synchronized (pagesToDb) {
            try {
                String shortLink = shortLink(url);

                if (pagesToDb.containsKey(shortLink)) {
                    return null;
                }

                connection.ignoreHttpErrors(true);
                Connection.Response response = connection.execute();
                int status = response.statusCode();
                if (response.statusMessage().equals("Not Found")) {
                    return null;
                }
                if (status != 200 && (response.contentType() == null
                        || !response.contentType().equals("text/html"))) {
                    return null;
                }
                Page page = new Page();
                page.setPath(shortLink);
                page.setCode(status);
                page.setContext(response.body());
                pagesToDb.put(shortLink, page);
                log.info("Ссылка - " + shortLink);
                return page;
            } catch (IOException e) {
                log.error("Ошибка подключения: " + e.getMessage());
                return null;
            }
        }
    }

    private Map<Lemma, Integer> addLemmas(Document doc) throws IOException {
        synchronized (lemmaToDb) {
            Map<Lemma, Integer> threadLemma = new HashMap<>();

            for (Field field : fields) {
                Elements select = doc.select(field.getSelector());
                String cleanPage = Jsoup.clean(select.toString(), Safelist.none());

                Map<String, Integer> foundLemmas = Lemmatizer.getLemmatizer().doLemma(cleanPage);
                foundLemmas.forEach((foundLemma, amount) -> {
                    Lemma lemma;
                    if (!lemmaToDb.containsKey(foundLemma)) {
                        lemma = new Lemma();
                        lemma.setFrequency(1);
                        lemma.setLemma(foundLemma);
                    } else {
                        lemma = lemmaToDb.get(foundLemma);
                        if (threadLemma.containsKey(lemma)) return;

                        int freq = lemma.getFrequency();
                        lemma.setFrequency(freq + 1);
                    }
                    threadLemma.put(lemma, amount);
                    lemmaToDb.put(foundLemma, lemma);
                });
            }
            return threadLemma;
        }
    }

    private Set<Index> addIndex(Page page, Map<Lemma, Integer> lemmaMap) {
        synchronized (indexToDb) {
            Map<Pair<String, String>, Index> indexesThread = new ConcurrentHashMap<>();
            for (Field field : fields) {
                lemmaMap.forEach((lemma, amount) -> {
                    Pair<String, String> key = Pair.of(shortLink(url), lemma.getLemma());
                    Index index = indexesThread.get(key);
                    if (index != null) index.setRank(index.getRank() + field.getWeight() * amount);
                    else {
                        index = new Index();
                        index.setPage(page);
                        index.setLemma(lemma);
                        index.setRank(field.getWeight() * amount);
                    }
                    indexesThread.put(key, index);
                });
            }
            indexToDb.putAll(indexesThread);

            return new HashSet<>(indexesThread.values());
        }
    }


    public List<Index> getIndexToDb() {
        return new CopyOnWriteArrayList<>(indexToDb.values());
    }

    public static void setIsCanceled(boolean isCanceled) {
        Parser.isCanceled = isCanceled;
    }

    public int countPages() {
        return pagesToDb.size();
    }

    public int countLemmas() {
        return lemmaToDb.size();
    }
}
