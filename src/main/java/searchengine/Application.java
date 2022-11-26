package searchengine;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.RequestParam;
import searchengine.dto.search.SearchResponse;
import searchengine.engine.Parser;
import searchengine.engine.Searcher;
import searchengine.services.IndexServiceImpl;
import searchengine.services.SearchServiceImpl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SpringBootApplication
public class Application {
    public static void main(String[] args) throws SQLException, IOException {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);

        IndexServiceImpl index = context.getBean(IndexServiceImpl.class);
        SearchServiceImpl search = context.getBean(SearchServiceImpl.class);
//        index.indexPage("https://playback.ru");
//        index.startIndexing();
        SearchResponse response = search.search("прочность радио","https://playback.ru",0,20);
        System.out.println(response);


//                Thread t = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(10_000);
//                    index.stopIndexing();
//                    System.out.println("Стоямба");
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        });
//        t.start();
    }
}
