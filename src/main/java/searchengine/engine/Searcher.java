package searchengine.engine;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.RelevantPage;
import searchengine.repository.LemmaRepository;


import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Searcher {
    private final List<RelevantPage> foundPages;
    private final List<Lemma> uniqueLemma;
    private final LemmaRepository lRepository;

    public Searcher(String request, LemmaRepository lRepository, long siteId) throws IOException {
        this.lRepository = lRepository;

        List<String> searchRequestLemmas = searcherLemmatizer(request);
        uniqueLemma = foundRequestLemmas(searchRequestLemmas, siteId);
        List<Page> pages = foundPages(uniqueLemma);
        if (!pages.isEmpty()) {
            foundPages = relevantPages(pages, searchRequestLemmas);
        } else {
            foundPages = new ArrayList<>();
            System.out.println("Поиск не дал результатов");
        }
    }

    public List<RelevantPage> getFoundPages() {
        return foundPages;
    }


    //разбиваем поисковый запрос на слова и возращаем их
    public List<String> searcherLemmatizer(String request) throws IOException {
        List<String> result = new ArrayList<>();
        HashMap<String, Integer> lemmas = Lemmatizer.getLemmatizer().doLemma(request);
        lemmas = sortByValue(lemmas);
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            result.add(entry.getKey());
        }
        Collections.reverse(result);
        return result;
    }

    //Вытягиваем из базы леммы соответствующие запросу
    //условия отбора frequency < 100
    public List<Lemma> foundRequestLemmas(List<String> requests, long site_id) {
        List<Lemma> result = new ArrayList<>();
        for (String lemma : requests) {
            result.addAll(lRepository.findLemmaByName(site_id, lemma));
        }
        return result;
    }

    //Формируем список страницы на которых найдены все слова из нашего запроса
    public List<Page> foundPages(List<Lemma> lemmas) {
        List<Page> result = new ArrayList<>();
        if (!lemmas.isEmpty()) {
            result = lemmas.get(0).getIndexes().stream().map(Index::getPage).collect(Collectors.toList());
        }

        int count = 1;
        if (lemmas.size() > 1)
            while (count != lemmas.size()) {
                List<Page> tempPages = result;
                result = lemmas.get(count).getIndexes().stream().map(Index::getPage)
                        .filter(tempPages::contains)
                        .collect(Collectors.toList());
                if (result.isEmpty()) break;
                count++;
            }
        return result;
    }


    //возвращаем релеватные страницы сортитированные по
    public List<RelevantPage> relevantPages(List<Page> pages, List<String> searchRequestLemmas) {
        float[] tempRelevance = new float[pages.size()];
        float maxPageRelevance = 0;
        List<RelevantPage> relevantPages = new ArrayList<>();
        int count = 0;
        for (Page p : pages) {
            float absRelevance = p.getIndexes().stream().map(x -> x.getRank()).reduce(0.0f, (x, y) -> x + y);
            tempRelevance[count] = absRelevance;
            count++;
        }
        Arrays.sort(tempRelevance);
        maxPageRelevance = tempRelevance[tempRelevance.length - 1];
        int count2 = 0;
        for (Page page : pages) {
            RelevantPage relevantPage = new RelevantPage();
            relevantPage.setUri(page.getPath());
            try {
                String title = Jsoup.parse(page.getContext()).getElementsByTag("title").get(0).text();

                relevantPage.setTitle(title);
            } catch (IndexOutOfBoundsException e) {
                relevantPage.setTitle("title not found");
            }
            relevantPage.setSite(page.getSite().getUrl().substring(0, page.getSite().getUrl().lastIndexOf("/")));
            relevantPage.setName(page.getSite().getName());
            relevantPage.setRelevance(tempRelevance[count2] / maxPageRelevance);
            relevantPage.setSnippet(getSnippet(page, searchRequestLemmas));
            relevantPages.add(relevantPage);
            count2++;
        }
        Collections.sort(relevantPages);

        return relevantPages;
    }

    public String getSnippet(Page page, List<String> searchRequestLemmas) {
        Document document = Jsoup.parse(page.getContext());
        List<Element> elements = new ArrayList<>();
        document.forEach(elements::add);

        List<String> cleanedHtml = new ArrayList<>();
        for (Element e : elements) {
            String cleanElement = Jsoup.clean(e.toString(), Safelist.none());
            if (cleanElement.length() > 1)
                cleanedHtml.add(cleanElement);
        }

        StringBuilder result = new StringBuilder();

        for (String cleaned : cleanedHtml) {
            for (String request : searchRequestLemmas) {
                int start = cleaned.indexOf(request);
                if (start >= 0) {
                    int end = start + request.length();
                    String snippet = cleaned.replace(request, "<b>" + request + "</b>");
                    snippet = snippet.substring(Math.max(0, start - 30), Math.min(end + 30, snippet.length()));
                    result.append(snippet + "\n");
                }
            }
        }
        return result.toString();
    }

    //сортировка мапы по значению. Используется для сортировки лемм начиная с самой редкой(по частоте появления в запросе)
    public static <K, V extends Comparable<? super V>> HashMap<K, V>
    sortByValue(HashMap<K, V> map) {
        HashMap<K, V> result = new LinkedHashMap<>();
        Stream<Map.Entry<K, V>> st = map.entrySet().stream();

        st.sorted(Map.Entry.comparingByValue())
                .forEach(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }
    public List<Lemma> getUniqueLemma() {
        return uniqueLemma;
    }
    public LemmaRepository getlRepository() {
        return lRepository;
    }
}
