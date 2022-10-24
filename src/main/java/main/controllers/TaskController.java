package main.controllers;

import main.modal.*;
import main.modal.search.ResultSearch;
import main.modal.search.ResultSearchError;
import main.modal.search.SearchQuery;
import main.modal.statistics.ResultStatistics;
import main.modal.statistics.Statistics;
import main.modal.statistics.StatisticsTotal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class TaskController {
    public static AtomicBoolean isIndexing = new AtomicBoolean(false);
    SearchQuery searchQuery = new SearchQuery();
    @Autowired
    Datasource datasource;
    @Autowired
    SitesProperties sitesPropNew;

    @Value("${userAgent}")
    public String userAgent;

    @RequestMapping(value = {"/statistics", "/api/statistics"}, method = RequestMethod.GET)
    public ResultStatistics statistic() throws SQLException {
        Statistics statistics = new Statistics();
        ResultStatistics resultStatistics = new ResultStatistics();
        StatisticsTotal total = ConnectionMySql.statisticsTotal();
        if (isIndexing.get()) {
            total.setIsIndexing("true");
        } else {
            total.setIsIndexing("");
        }
        statistics.setTotal(total);
        statistics.setDetailed(ConnectionMySql.statisticsDetailed());
        resultStatistics.setResult("true");
        resultStatistics.setStatistics(statistics);
        return resultStatistics;
    }

    @RequestMapping(value = {"/startIndexing", "/api/startIndexing"}, method = RequestMethod.GET)
    public ResultStartIndexing startIndexing() {
        List<SitesPogo> sites4 = sitesPropNew.getSites();
        ResultStartIndexing resultStartIndexing = new ResultStartIndexing();
        CreateThreadsIndexing createThreadsIndexing = new CreateThreadsIndexing();
        resultStartIndexing = new ResultStartIndexing();
        createThreadsIndexing = new CreateThreadsIndexing();
        if (isIndexing.get()) {
            resultStartIndexing.setResult("false");
            resultStartIndexing.setError("Индексация уже запущена");
        } else {
            resultStartIndexing.setResult("true");
            createThreadsIndexing.createThreadsIndexingAll(sites4, userAgent);
        }
        return resultStartIndexing;
    }

    @RequestMapping(value = {"/stopIndexing", "/api/stopIndexing"}, method = RequestMethod.GET)
    public ResultStartIndexing stopIndexing() throws SQLException {
        ResultStartIndexing resultStartIndexing = new ResultStartIndexing();
        CreateThreadsIndexing createThreadsIndexing = new CreateThreadsIndexing();
        if (!isIndexing.get()) {
            resultStartIndexing.setResult("false");
            resultStartIndexing.setError("Индексация не запущена");
        } else {
            resultStartIndexing.setResult("true");
            createThreadsIndexing.stopIndexingAll();
        }
        return resultStartIndexing;
    }

    @RequestMapping(value = {"/indexPage", "/api/indexPage"}, method = RequestMethod.POST)
    @ResponseBody
    public ResultStartIndexing startIndexingPage(@RequestParam(name = "url") String url) throws SQLException {
        ResultStartIndexing resultStartIndexing = new ResultStartIndexing();
        CreateThreadsIndexing createThreadsIndexing = new CreateThreadsIndexing();
        String root = ConnectionMySql.getSiteRoot(url);
        if (root.isEmpty()) {
            resultStartIndexing.setResult("false");
            resultStartIndexing.setError("Данная страница находится за пределами сайтов, \n" +
                    "указанных в конфигурационном файле\n");
        } else {
            resultStartIndexing.setResult("true");
            createThreadsIndexing.createThreadsIndexingPage(url, root, userAgent);
        }
        return resultStartIndexing;
    }

    @RequestMapping(value = {"/search", "/api/search"}, method = RequestMethod.GET)
    @ResponseBody
    public Object search(@RequestParam(name = "query") String query,
                         @RequestParam(name = "offset") int offset,
                         @RequestParam(name = "limit") int limit,
                         @RequestParam(name = "site", defaultValue = "") String site)
            throws SQLException {
        ResultSearch resultSearch = new ResultSearch();
        if (query.trim().isEmpty()) {
            ResultSearchError resultSearchError = new ResultSearchError();
            resultSearchError.setResult("false");
            resultSearchError.setError("Задан пустой поисковый запрос");
            return resultSearchError;
        } else {
            if (ConnectionMySql.getIndexing(site)) {
                resultSearch = searchQuery.searchQueryOffset(offset, limit, query, site);
            } else {
                ResultSearchError resultSearchError = new ResultSearchError();
                resultSearchError.setResult("false");
                resultSearchError.setError("Сайт не проиндексирован");
                return resultSearchError;
            }
        }
        return resultSearch;
    }
}
