package main.modal;

import main.modal.indexing.Indexing;
import main.modal.indexing.IsIndexingFalse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static main.controllers.PagesController.isIndexing;

@Component
public class CreateThreadsIndexing {
    static List<Thread> list = new ArrayList<>();
    ConnectionMySql connectionMySql;

    private static final Logger log = LogManager.getLogger(CreateThreadsIndexing.class);

    public CreateThreadsIndexing() {
    }

    public void createThreadsIndexingAll(List<SitesPogo> sites4, String userAgent) {
        list = new ArrayList<>();
        isIndexing.set(true);
        for (SitesPogo site : sites4) {
            site.setRoot(site.getUrl());
            Runnable runnable = new Indexing(site, userAgent, true);
            Thread thread = new Thread(runnable);
            thread.setName(site.getRoot());
            thread.start();
            list.add(thread);
            log.info("Indexing site: " + site.getRoot());
        }
        Runnable runnable1 = new IsIndexingFalse(list);
        Thread thread1 = new Thread(runnable1);
        thread1.start();
    }

    public void stopIndexingAll() {
        isIndexing.set(false);
        list.forEach(t -> {
            try {
                connectionMySql.saveStatus("FAILED", t.getName(),
                        "Ошибка: выполнение парсинга сайта прервано");
                t.stop();
            } catch (SQLException e) {
                log.error(e);
                throw new RuntimeException(e);
            }
        });
    }

    public void createThreadsIndexingPage(String url, String root, String userAgent) {
        list = new ArrayList<>();
        SitesPogo site = new SitesPogo();
        site.setUrl(url);
        site.setRoot(root);
        isIndexing.set(true);
        Runnable runnable = new Indexing(site, userAgent, false);
        Thread thread = new Thread(runnable);
        thread.setName(root);
        thread.start();
        list.add(thread);
        Runnable runnable1 = new IsIndexingFalse(list);
        Thread thread2 = new Thread(runnable1);
        thread2.start();
    }
}
