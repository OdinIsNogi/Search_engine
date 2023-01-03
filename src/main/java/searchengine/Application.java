package searchengine;


import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import searchengine.dto.search.SearchResponse;
import searchengine.engine.Parser;
import searchengine.services.IndexServiceImpl;
import searchengine.services.SearchServiceImpl;

import java.io.IOException;
import java.sql.SQLException;

@Slf4j
@SpringBootApplication
public class Application {
    public static void main(String[] args) throws SQLException, IOException {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);

        IndexServiceImpl index = context.getBean(IndexServiceImpl.class);
        SearchServiceImpl search = context.getBean(SearchServiceImpl.class);

//        parser.compute();
//        index.indexPage("https://skillbox.ru/media/design/Shinolebedi/-4");
        index.startIndexing();


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
