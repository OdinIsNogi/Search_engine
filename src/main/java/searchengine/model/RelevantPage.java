package searchengine.model;

import lombok.Data;

@Data
public class RelevantPage implements Comparable<RelevantPage> {
    String site;
    String name;
    String uri;
    String title;
    String snippet;
    Float relevance;


    @Override
    public int compareTo(RelevantPage o) {
        return o.getRelevance().compareTo(relevance);
    }
}
