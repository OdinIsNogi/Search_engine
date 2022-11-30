package searchengine;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import searchengine.dto.search.SearchResponse;

import searchengine.services.IndexServiceImpl;
import searchengine.services.SearchServiceImpl;

import java.io.IOException;
import java.sql.SQLException;


@SpringBootApplication
public class Application {
    public static void main(String[] args) throws SQLException, IOException {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);

        IndexServiceImpl index = context.getBean(IndexServiceImpl.class);
        SearchServiceImpl search = context.getBean(SearchServiceImpl.class);
//        index.indexPage("https://playback.ru");
//        index.startIndexing();
//        SearchResponse response = search.search("прочность радио", "https://playback.ru", 0, 20);
//        System.out.println(response);


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
