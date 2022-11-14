package main.modal.indexing;


import main.modal.ConnectionMySql;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

import static main.controllers.PagesController.isIndexing;

@Component
public class IsIndexingFalse implements Runnable {
    List<Thread> list;

    private static final Logger log = LogManager.getLogger(IsIndexingFalse.class);

    public IsIndexingFalse(List<Thread> list){
        this.list = list;
    }


    @Override
    public void run() {
        list.forEach(t -> {
            try {
                t.join();
                if (t.isInterrupted()) {
                    log.info("Site parsing aborted: " + t.getName());
                    ConnectionMySql.saveStatus("FAILED", t.getName(),
                            "Ошибка: выполнение парсинга сайта прервано");
                } else {
                    if (ConnectionMySql.getStatus(t.getName()).equals("INDEXING") &&
                            ConnectionMySql.getTestParsingSite(t.getName())) {
                        ConnectionMySql.saveStatus("INDEXED", t.getName(), "");
                        log.info(t.getName() + " INDEXED");
                    } else {
                        ConnectionMySql.saveStatus("FAILED", t.getName(),
                                "Ошибка: выполнение парсинга сайта прервано");
                    }
                }
            } catch (InterruptedException | SQLException e) {
                log.error(e);
                throw new RuntimeException(e);
            }
        });
        isIndexing.set(false);
    }
}
