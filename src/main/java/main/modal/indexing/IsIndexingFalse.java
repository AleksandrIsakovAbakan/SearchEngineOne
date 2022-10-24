package main.modal.indexing;

import main.modal.ConnectionMySql;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

import static main.controllers.TaskController.isIndexing;

@Component
public class IsIndexingFalse implements Runnable {
    List<Thread> list;

    public IsIndexingFalse(List<Thread> list) {
        this.list = list;
    }


    @Override
    public void run() {
        list.forEach(t -> {
            try {
                t.join();
                if (t.isInterrupted()) {
                    ConnectionMySql.saveStatus("FAILED", t.getName(),
                            "Ошибка: выполнение парсинга сайта прервано");
                } else {
                    if (ConnectionMySql.getStatus(t.getName()).equals("INDEXING") &&
                            ConnectionMySql.getTestParsingSite(t.getName())) {
                        ConnectionMySql.saveStatus("INDEXED", t.getName(), "");
                        System.out.println(t.getName() + " INDEXED");
                    } else {
                        ConnectionMySql.saveStatus("FAILED", t.getName(),
                                "Ошибка: выполнение парсинга сайта прервано");
                    }
                }
            } catch (InterruptedException | SQLException e) {
                throw new RuntimeException(e);
            }
        });
        isIndexing.set(false);
    }
}
