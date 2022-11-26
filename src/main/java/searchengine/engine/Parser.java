package searchengine.engine;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.springframework.data.util.Pair;
import searchengine.model.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

import static java.lang.Thread.sleep;

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
        List<Parser> tasks = new ArrayList<>();
        Connection connection = Jsoup.connect(url).timeout(10 * 1000)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("https://www.google.com")
                .followRedirects(true);

        Page curPage = addPage(connection);
        if (curPage == null) return;
        try {
            sleep(100);

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
//                    log.info("Temp: " + child);
                }
            }
        } catch (IOException | InterruptedException e) {
            log.error("ERROR during parsing" + url);
        }
        ForkJoinTask.invokeAll(tasks);

    }

    //добавляем страницы
    private Page addPage(Connection connection) throws IOException {
        synchronized (pagesToDb) {
            String shortLink = shortLink();
            if (shortLink.equals("")) {
                shortLink = root;
            }
            if (pagesToDb.containsKey(shortLink)) return null;
            Page page = new Page();
            page.setPath(shortLink);

            try {
                connection.ignoreHttpErrors(true);
                Connection.Response response = connection.execute();
                int status = response.statusCode();
                if (status != 200 && (response.contentType() == null || !response.contentType().startsWith("html/text")))
                    throw new UnsupportedMimeTypeException("Bad connection", "unsupported type", response.url().toString());

                page.setCode(status);
                page.setContext(response.body());
                pagesToDb.put(shortLink, page);
                log.info("Ссылка - " + shortLink);
                return page;
            } catch (UnsupportedMimeTypeException mimeTypeEx) {
                log.warn(mimeTypeEx.getUrl() + " - unsupported type: photo,gif, etc.");
                return null;
            } catch (IOException e) {
                log.error("During addPage: " + e.getMessage());
                return null;
            }
        }
    }

    private Map<Lemma, Integer> addLemmas(Document doc) throws IOException {
        synchronized (lemmaToDb) {
            Map<Lemma, Integer> threadLemma = new HashMap<>();

            for (Field field : fields) {
//                log.info("Field: " + field);
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
            if (page.getCode() == 200) {
                for (Field field : fields) {
                    lemmaMap.forEach((lemma, amount) -> {
                        Pair<String, String> key = Pair.of(shortLink(), lemma.getLemma());
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
            }
            return new HashSet<>(indexesThread.values());
        }
    }

    private String shortLink() {
        return url.replace(root, "");
    }

    //&& !child.matches(".+\\.[A-z]{1,5}$
    public boolean isCorrect(String child) {
        return child.contains(root) && !child.contains("#") && !child.contains("?");
    }

    public ArrayList<Index> getIndexToDb() {
        return new ArrayList<>((indexToDb.values()));
    }

    public static void setIsCanceled(boolean isCanceled) {
        Parser.isCanceled = isCanceled;
    }
}
