package main.modal.indexing;

import main.modal.ConnectionMySql;
import main.modal.SitesPogo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;


public class Indexing implements Runnable {
    SitesPogo site;
    boolean pageAll;
    public String userAgent;

    private static final Logger log = LogManager.getLogger(Indexing.class);

    public Indexing(SitesPogo site, String userAgent, boolean pageAll) {
        this.site = site;
        this.userAgent = userAgent;
        this.pageAll = pageAll;
    }

    @Override
    public void run() {
        String root = site.getRoot();
        String url = site.getUrl();
        int siteId = 0;
        try {
            siteId = siteIdSave(root, url, pageAll);
        } catch (SQLException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
        if (!url.substring(url.length() - 1).equals("/")) { url = url + "/";}
        if (!root.substring(root.length() - 1).equals("/")) { root = root + "/";}
        Set<String> link = new HashSet<>();
        Set<MapRecursiveTask> taskList = new HashSet<>();
        Object[] args = {url, root, siteId, link, userAgent, pageAll};
        taskList = new ForkJoinPool().invoke(new MapRecursiveTask(args));
        for (MapRecursiveTask task : taskList) {
            task.join();
            if (task.isCancelled()) {
                try {
                    ConnectionMySql.saveStatus("FAILED", root, "Ошибка: выполнение парсинга сайта прервано");
                } catch (SQLException e) {
                    log.error(e);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public int siteIdSave(String root, String url, boolean pageAll) throws SQLException {
        int siteId = 0;
        try {
            if (pageAll) {
                siteId = ConnectionMySql.getSiteIdIndexing(root, url, pageAll);
                ConnectionMySql.saveName(site.getName(), site.getUrl());
            } else {
                siteId = ConnectionMySql.getSiteIdIndexing(root, url, pageAll);
            }
        } catch (SQLException e) {
            log.error(e);
            e.printStackTrace();
            ConnectionMySql.saveException(e.getMessage(), siteId);
        }
        return siteId;
    }
}
