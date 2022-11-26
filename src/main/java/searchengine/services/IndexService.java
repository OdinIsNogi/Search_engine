package searchengine.services;


import searchengine.model.Index;
import searchengine.dto.index.IndexResponse;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;


public interface IndexService {

    IndexResponse startIndexing() throws SQLException;

    Optional<Index> getIndexById(long id);

    Iterable<Index> getIndexList();

    void createIndex(Index index);

    void creatIndexSet(Collection<Index> indexes);

    void deleteIndex(long id);

    void deleteAllIndexes();
}
