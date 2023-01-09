package searchengine.engine;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@Slf4j
public class Lemmatizer {

    private static Lemmatizer lemmatizer;
    private final LuceneMorphology luceneMorphology;

    public static Lemmatizer getLemmatizer() throws IOException {
        if (lemmatizer == null) {
            LuceneMorphology morphology = new RussianLuceneMorphology();
            lemmatizer = new Lemmatizer(morphology);
        }
        return lemmatizer;
    }

    private Lemmatizer(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }


    public HashMap<String, Integer> doLemma(String text) throws IOException {
        String[] words = text.replaceAll("[^а-яА-Я]", " ").toLowerCase(Locale.ROOT).split(" +");
        HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            String lemmaWordForm = luceneMorphology.getNormalForms(word).get(0);
            if (!check(wordInfo(lemmaWordForm)) && lemmaWordForm.length() > 1) {
                if (lemmas.containsKey(lemmaWordForm)) {
                    lemmas.put(lemmaWordForm, lemmas.get(lemmaWordForm) + 1);
                } else {
                    lemmas.put(lemmaWordForm, 1);
                }
            }
        }
        return lemmas;
    }

    public String wordInfo(String word) {
        List<String> info = luceneMorphology.getMorphInfo(word);
        return info.get(0);
    }


    public boolean check(String word) {
        boolean ch1 = word.contains("СОЮЗ");
        boolean ch2 = word.contains("МЕЖД");
        boolean ch3 = word.contains("ПРЕДЛ");
        boolean ch4 = word.contains("ПРЕДК");
        boolean ch5 = word.contains("ЧАСТ");
        boolean ch6 = word.contains("МС");
        return ch1 || ch2 || ch3 || ch4 || ch5 || ch6;
    }
}
