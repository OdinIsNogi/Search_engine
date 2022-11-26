package searchengine.dto.search;

import lombok.Data;
import searchengine.model.RelevantPage;

import java.util.List;

@Data
public class SearchResponse {
    boolean response;
    int count;
    String error;
    List<RelevantPage> relevantPages;

    public SearchResponse(boolean response, int count, List<RelevantPage> relevantPages) {
        this.response = response;
        this.count = count;
        this.relevantPages = relevantPages;
    }

    public SearchResponse(boolean response, String error) {
        this.response = response;
        this.error = error;
    }
}
