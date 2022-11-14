package main.modal;

import main.modal.statistics.StatisticsDetailed;
import main.modal.statistics.StatisticsTotal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static main.controllers.PagesController.isIndexing;

@Service
public class ConnectionMySql {
    static float titleSql = 0F;
    static float bodySql = 0F;
    public static Connection connection;

    private static final Logger log = LogManager.getLogger(ConnectionMySql.class);

    public ConnectionMySql() {
    }

    public ConnectionMySql(Connection connection) throws SQLException {
        this.connection = connection;
    }

    public static void executeInsert(Object[] args) throws SQLException {
        String url = (String) args[0];
        int code = (int) args[1];
        Document docNew = (Document) args[2];
        int siteId = (int) args[3];
        String docNew1 = "";
        String url1 = "";
        if (docNew != null) {
            docNew1 = docNew.toString();
            docNew1 = docNew1.replaceAll("'", "\"");
        }
        if (docNew1.length() >= 16777215) {
            docNew1 = docNew1.substring(0, 16777214);
        }
        if (url.substring(url.length() - 1).equals("/") && url.length() > 1) {
            url1 = url.substring(0, url.length() - 1);
        } else {
            url1 = url;
        }
        String sql = "INSERT INTO page(path, code, content, site_id, sum_rank_lemma_page)" +
                " VALUES ('" + url1 + "', " + code + ", '" + docNew1 + "', " + siteId + "," + 0.0 + ")" +
                " ON DUPLICATE KEY UPDATE path=values(path)";
        connection.createStatement().executeUpdate(sql);
        connection.createStatement().close();
        String sqlSiteNew = "UPDATE site SET status_time=current_timestamp() WHERE id=" + siteId;
        connection.createStatement().executeUpdate(sqlSiteNew);
        connection.createStatement().close();
    }

    public static void executeInsertLemma(HashMap<String, Integer> insertLemmaTitle,
                                          HashMap<String, Integer> insertLemmaBody, int siteId)
            throws SQLException {
        HashMap<String, Integer> insertLemma = new HashMap<>();
        insertLemma.putAll(insertLemmaTitle);
        insertLemma.putAll(insertLemmaBody);
        if (insertLemma != null) {
            StringBuilder stringLemma = new StringBuilder();
            Set<String> key = insertLemma.keySet();
            for (String key1 : key) {
                stringLemma.append("('").append(key1).append("', 1, ").append(siteId).append("), ");
            }
            if (stringLemma.length() > 8) {
                stringLemma = new StringBuilder(stringLemma.substring(0, stringLemma.length() - 2));
                String sql = "INSERT INTO lemma(lemma, frequency, site_id) VALUES "
                        + stringLemma + " ON DUPLICATE KEY UPDATE frequency = frequency + 1";
                connection.createStatement().executeUpdate(sql);
                connection.createStatement().close();
            }
        }
    }

    public static void executeInsertIndex(Object[] args1) throws SQLException {
        HashMap<String, Integer> insertIndexTitle = (HashMap<String, Integer>) args1[0];
        HashMap<String, Integer> insertIndexBody = (HashMap<String, Integer>) args1[1];
        String urlStr = (String) args1[2];
        int siteId = (int) args1[3];
        HashMap<String, Double> insertIndex = createInsertIndex(insertIndexTitle, insertIndexBody);
        HashMap<String, Double> lemmaSql = new HashMap<>();
        if (urlStr.substring(urlStr.length() - 1).equals("/") && urlStr.length() > 1) {
            urlStr = urlStr.substring(0, urlStr.length() - 1);
        }
        String sql = "SELECT id, site_id FROM page WHERE path = '" + urlStr + "' AND site_id=" + siteId;
        int pageId = 0;
        ResultSet resultSet = connection.createStatement().executeQuery(sql);
        connection.createStatement().close();
        if (resultSet != null) {
            resultSet.next();
            if (resultSet.isFirst()) {
                pageId = resultSet.getInt("id");
                if (pageId > 0) {
                    Set<String> key1 = insertIndex.keySet();
                    for (String key : key1) {
                        double valueIndex = insertIndex.get(key).doubleValue();
                        lemmaSql.put(key, valueIndex);
                    }
                    insertKeyIndex(pageId, lemmaSql, siteId);
                } else {
                    log.error("ERROR pageId 0 " + urlStr);
                }
            }
        } else {
            log.error("ERROR resultSet null " + urlStr);
        }
        resultSet.close();
    }

    private static void insertKeyIndex(int pageId, HashMap<String, Double> lemmaSql,
                                       int siteId) throws SQLException {
        HashMap<String, Integer> searchString4 = new HashMap<>();
        int lemmaId = 0;
        ResultSet resultSet1 = lemmaSelect(lemmaSql, siteId);
        while (resultSet1.next()) {
            lemmaId = resultSet1.getInt("id");
            String lemmaName = resultSet1.getString("lemma");
            searchString4.put(lemmaName, lemmaId);
        }
        Set<String> key1 = searchString4.keySet();
        StringBuilder stringIdPageLemmaRank = new StringBuilder();
        float sumRankLemmaPage = 0;
        for (String key : key1) {
            sumRankLemmaPage = sumRankLemmaPage + searchString4.get(key).intValue();
            if (searchString4.get(key) != null && lemmaSql.get(key) != null) {
                stringIdPageLemmaRank.append("(").append(pageId).append(", ").append(searchString4.get(key).intValue())
                        .append(", ").append(lemmaSql.get(key)).append(", ").append(siteId).append("), ");
            }
        }
        stringIdPageLemmaRank = new StringBuilder(stringIdPageLemmaRank.
                substring(0, stringIdPageLemmaRank.length() - 2));
        String sql2 = "INSERT INTO index_lemma(page_id, lemma_id, rank_lemma_page, site_id) VALUES "
                + stringIdPageLemmaRank;
        connection.createStatement().executeUpdate(sql2);
        connection.createStatement().close();
        String sqlSumRankLemmaPage = "UPDATE page SET sum_rank_lemma_page=" + sumRankLemmaPage + " WHERE id="
                + pageId + " AND site_id=" +siteId;
        connection.createStatement().executeUpdate(sqlSumRankLemmaPage);
        connection.createStatement().close();
    }

    public static HashMap<String, Double> createInsertIndex(HashMap<String, Integer> insertIndexTitle,
                                                            HashMap<String, Integer> insertIndexBody) {
        HashMap<String, Double> insertIndexNew = new HashMap<>();
        Set<String> keyAll = insertIndexBody.keySet();
        Set<String> keyAllTitle = insertIndexTitle.keySet();
        Set<String> keyAll1 = new HashSet<>();
        keyAll1.addAll(keyAllTitle);
        keyAll1.addAll(keyAll);
        for (String keyA : keyAll1) {
            double value = 0;
            if (insertIndexBody.containsKey(keyA)) {
                value = insertIndexBody.get(keyA).intValue() * bodySql;
                if (insertIndexTitle.containsKey(keyA)) {
                    value = value + insertIndexTitle.get(keyA).intValue() * titleSql;
                }
            } else {
                value = insertIndexTitle.get(keyA).intValue() * titleSql;
            }
            insertIndexNew.put(keyA, value);
        }
        return insertIndexNew;
    }

    public static ArrayList<Page> searchQueryProcessingSql(String stringIndex, int siteId) throws SQLException {
        ArrayList<Page> page2 = new ArrayList<>();
        String searchStringSqlKey3;
        if (siteId > 0) {
            searchStringSqlKey3 = "SELECT lemma_id, page_id, rank_lemma_page, site_id FROM index_lemma WHERE site_id="
                    + siteId + " AND (" + stringIndex + ")";
        } else {
            searchStringSqlKey3 = "SELECT lemma_id, page_id, rank_lemma_page, site_id FROM index_lemma WHERE "
                    + stringIndex;
        }
        ResultSet resultSet2 = connection.createStatement().executeQuery(searchStringSqlKey3);
        while (resultSet2.next()) {
            int lemmaId = resultSet2.getInt("lemma_id");
            int pageId = resultSet2.getInt("page_id");
            int siteIdSearch = resultSet2.getInt("site_id");
            float rankLemmaPage = resultSet2.getFloat("rank_lemma_page");
            Object[] args3 = {lemmaId, pageId, rankLemmaPage, siteIdSearch};
            page2.add(new Page(args3));
        }
        return page2;
    }

    public static HashMap<Integer, Float> relevanceCalculationSql(HashMap<Integer,
            Float> searchStringRelevance) throws SQLException {
        HashMap<Integer, Float> searchStringRelevance1 = new HashMap<>();
        if (searchStringRelevance.size() > 0) {
            String searchSqlRelevance = "";
            float sumRankLemmaPage = 0;
            for (int key : searchStringRelevance.keySet()) {
                searchSqlRelevance = searchSqlRelevance + "id=" +key + " OR ";
            }
            if (searchSqlRelevance.length() > 4) {
                searchSqlRelevance = searchSqlRelevance.substring(0, searchSqlRelevance.length() - 4);
            }
            String searchStringSqlRelevance = "SELECT id, sum_rank_lemma_page FROM page WHERE " + searchSqlRelevance;
            ResultSet resultSetRelevance = connection.createStatement().executeQuery(searchStringSqlRelevance);
            connection.createStatement().close();
            while (resultSetRelevance.next()) {
                sumRankLemmaPage = resultSetRelevance.getFloat("sum_rank_lemma_page");
                int pageId = resultSetRelevance.getInt("id");
                searchStringRelevance1.put(pageId, sumRankLemmaPage);
            }
            resultSetRelevance.close();
        }
        return searchStringRelevance1;
    }

    public static ResultSet responseOutputSql(Integer key, int siteId) throws SQLException, IOException {
        String responseOutputSqlNamePage = "";
        if (siteId != 0) {
            responseOutputSqlNamePage = "SELECT path, content FROM page WHERE id = " + key
                    + " AND site_id=" + siteId;
        } else {
            responseOutputSqlNamePage = "SELECT path, content, site_id FROM page WHERE id = " + key;
        }
        ResultSet resultOutputSqlNamePage = connection.createStatement().executeQuery(responseOutputSqlNamePage);
        connection.createStatement().close();
        return resultOutputSqlNamePage;
    }

    public static int getSiteIdIndexing(String site, String sitePage, boolean pageAll) throws SQLException {
        int siteId = 0;
        String sqlSiteId;
        ResultSet resultSiteId;
        if (pageAll) {
            sqlSiteId = "SELECT id FROM site WHERE url='" + site + "'";
            resultSiteId = connection.createStatement().executeQuery(sqlSiteId);
            connection.createStatement().close();
            if (!resultSiteId.next()) {
                String sqlSiteNew = "INSERT INTO site(status, status_time, last_error, url, name) VALUES ('INDEXING'," +
                        "current_timestamp(), NULL, '" + site + "', '')";
                connection.createStatement().executeUpdate(sqlSiteNew);
                connection.createStatement().close();
                resultSiteId = connection.createStatement().executeQuery(sqlSiteId);
                connection.createStatement().close();
                resultSiteId.next();
                siteId = resultSiteId.getInt("id");
            } else {
                siteId = resultSiteId.getInt("id");
                String sqlSiteOld = "UPDATE site SET last_error='', status='INDEXING', status_time=current_timestamp()" +
                        " WHERE id=" + siteId;
                connection.createStatement().executeUpdate(sqlSiteOld);
                connection.createStatement().close();
            }
            resultSiteId.close();
        } else {
            siteId = getSiteIdIndexingPage(site);
        }
        if (siteId != 0) {
            if (pageAll) {
                deleteSiteId(siteId);
            } else {
                searchDeletePage(siteId, site, sitePage);
            }
        }
        return siteId;
    }

    public static int getSiteIdIndexingPage(String site) throws SQLException {
        int siteId = 0;
        ResultSet resultSiteIdPage;
        String root = getSiteRoot(site);
        String sqlSiteIdPage = "SELECT id FROM site WHERE url='" + root + "'";
        resultSiteIdPage = connection.createStatement().executeQuery(sqlSiteIdPage);
        connection.createStatement().close();
        if (!resultSiteIdPage.next()) {
            log.error("Ошибка: не найден siteId " + site);
            siteId = 0;
        } else {
            siteId = resultSiteIdPage.getInt("id");
            String sqlSiteOld = "UPDATE site SET last_error='', status='INDEXING', status_time=current_timestamp()" +
                    " WHERE id=" + siteId;
            connection.createStatement().executeUpdate(sqlSiteOld);
            connection.createStatement().close();
        }
        resultSiteIdPage.close();
        return siteId;
    }

    public static void deleteSiteId(int siteId) throws SQLException {
        String sqlDeletePage = "DELETE FROM page WHERE site_id=" + siteId;
        connection.createStatement().executeUpdate(sqlDeletePage);
        connection.createStatement().close();
        String sqlDeleteLemma = "DELETE FROM lemma WHERE site_id=" + siteId;
        connection.createStatement().executeUpdate(sqlDeleteLemma);
        connection.createStatement().close();
        String sqlDeleteIndex = "DELETE FROM index_lemma WHERE site_id=" + siteId;
        connection.createStatement().executeUpdate(sqlDeleteIndex);
        connection.createStatement().close();
    }

    public static void searchDeletePage(int siteId, String site, String sitePage) throws SQLException {
        int pageId, lemmaId = 0;
        String deletePageSql = "SELECT id, path FROM page WHERE site_id = " + siteId;
        String deletePagesSql = "";
        String deletePagesSql1 = "";
        String deleteLemmasSql = "";
        ResultSet resultDeletePageSql = connection.createStatement().executeQuery(deletePageSql);
        while (resultDeletePageSql.next()) {
            pageId = resultDeletePageSql.getInt("id");
            String pathPage = resultDeletePageSql.getString("path");
            if (pathPage.contains(sitePage.substring(site.length() - 1))) {
                deletePagesSql = deletePagesSql + "page_id=" + pageId + " OR ";
                deletePagesSql1 = deletePagesSql1 + "id=" + pageId + " OR ";
            }
        }
        connection.createStatement().close();
        resultDeletePageSql.close();
        if (deletePagesSql.length() > 12) {
            deletePagesSql = deletePagesSql.substring(0, deletePagesSql.length() - 3);
            deletePagesSql1 = deletePagesSql1.substring(0, deletePagesSql1.length() - 3);
            String deletePageLemmaSql = "SELECT lemma_id FROM index_lemma WHERE site_id="
                    + siteId + " AND (" + deletePagesSql + ")";
            ResultSet resultDeletePageLemmaSql = connection.createStatement().executeQuery(deletePageLemmaSql);
            while (resultDeletePageLemmaSql.next()) {
                lemmaId = resultDeletePageLemmaSql.getInt("lemma_id");
                deleteLemmasSql = deleteLemmasSql + "id=" + lemmaId + " OR ";
            }
            connection.createStatement().close();
            resultDeletePageLemmaSql.close();
            if (deleteLemmasSql.length() > 3) {
                deleteLemmasSql = deleteLemmasSql.substring(0, deleteLemmasSql.length() - 4);
            }
            Object[] args2 = {siteId, deletePagesSql, deleteLemmasSql, deletePagesSql1};
            deletePage(args2);
        }
    }

    public static void deletePage(Object[] args2)throws SQLException {
        int siteId = (int) args2[0];
        String deletePagesSql = (String) args2[1];
        String deleteLemmasSql = (String) args2[2];
        String deletePagesSql1 = (String) args2[3];

        String sqlDeleteLemma = "DELETE FROM lemma WHERE site_id=" + siteId + " AND (" + deleteLemmasSql + ")";
        String sqlDeleteIndex = "DELETE FROM index_lemma WHERE site_id=" + siteId + " AND ("
                + deletePagesSql + ")";
        String sqlDeletePage = "DELETE FROM page WHERE site_id=" + siteId + " AND ("
                + deletePagesSql1 + ")";
        if (deleteLemmasSql.length() > 3) {
            connection.createStatement().executeUpdate(sqlDeleteLemma);
            connection.createStatement().close();
        }
        connection.createStatement().executeUpdate(sqlDeleteIndex);
        connection.createStatement().close();
        connection.createStatement().executeUpdate(sqlDeletePage);
        connection.createStatement().close();
    }

    public static int getSiteIdSearch(String site) throws SQLException {
        int siteId = 0;
        String sqlSiteId = "SELECT id FROM site WHERE url='" + site + "'";
        ResultSet resultSiteId = connection.createStatement().executeQuery(sqlSiteId);
        connection.createStatement().close();
        if (resultSiteId.next()) {
            siteId = resultSiteId.getInt("id");
        }
        resultSiteId.close();
        return siteId;
    }

    public static ResultSet lemmaSelect(HashMap<String, Double> searchStringLemmaSql, int siteId) throws SQLException {
        Set<String> key1 = searchStringLemmaSql.keySet();
        StringBuilder stringIdLemma = new StringBuilder();
        String searchStringLemmaSql1 = "";
        for (String key : key1) {
            stringIdLemma.append(" lemma='").append(key).append("' OR ");
        }
        if (siteId > 0) {
            searchStringLemmaSql1 = "SELECT id, lemma, frequency, site_id FROM lemma WHERE site_id="
                    + siteId + " AND (" + stringIdLemma + ")";
        } else {
            searchStringLemmaSql1 = "SELECT id, lemma, frequency, site_id FROM lemma WHERE ("
                    + stringIdLemma;
        }
        searchStringLemmaSql1 = searchStringLemmaSql1.substring(0, searchStringLemmaSql1.length() - 4) + ")";
        return connection.createStatement().executeQuery(searchStringLemmaSql1);
    }

    public static void saveException(String message, int siteId) throws SQLException {
        String sqlException = "UPDATE site SET last_error='" + message + "' WHERE id=" + siteId;
        connection.createStatement().executeUpdate(sqlException);
        connection.createStatement().close();
    }

    public static StatisticsTotal statisticsTotal() throws SQLException {
        StatisticsTotal statisticsTotal = new StatisticsTotal();
        String sqlSite = "SELECT COUNT(*) FROM site";
        ResultSet resultSite = connection.createStatement().executeQuery(sqlSite);
        connection.createStatement().close();
        resultSite.next();
        statisticsTotal.setSites(resultSite.getInt("count(*)"));
        resultSite.close();
        String sqlPage = "SELECT COUNT(*) FROM page";
        ResultSet resultPage = connection.createStatement().executeQuery(sqlPage);
        connection.createStatement().close();
        resultPage.next();
        statisticsTotal.setPages(resultPage.getInt("count(*)"));
        resultPage.close();
        String sqlLemma = "SELECT COUNT(*) FROM lemma";
        ResultSet resultLemma = connection.createStatement().executeQuery(sqlLemma);
        connection.createStatement().close();
        resultLemma.next();
        statisticsTotal.setLemmas(resultLemma.getInt("count(*)"));
        resultLemma.close();
        return statisticsTotal;
    }

    public static ArrayList<StatisticsDetailed> statisticsDetailed() throws SQLException {
        ArrayList<StatisticsDetailed> statisticsDetaileds = new ArrayList<>();
        String sqlSiteDetailed = "SELECT * FROM site";
        ResultSet resultSiteDetailed = connection.createStatement().executeQuery(sqlSiteDetailed);
        connection.createStatement().close();
        while (resultSiteDetailed.next()) {
            StatisticsDetailed statisticsDetailed = new StatisticsDetailed();
            statisticsDetailed.setId(resultSiteDetailed.getInt("id"));
            statisticsDetailed.setUrl(resultSiteDetailed.getString("url"));
            statisticsDetailed.setName(resultSiteDetailed.getString("name"));
            statisticsDetailed.setStatus(resultSiteDetailed.getString("status"));
            statisticsDetailed.setStatusTime(resultSiteDetailed.getTimestamp("status_time"));
            statisticsDetailed.setError(resultSiteDetailed.getString("last_error"));
            String sqlPageId = "SELECT COUNT(*) FROM page WHERE site_id=" +
                    statisticsDetailed.getId();
            ResultSet resultPageId = connection.createStatement().executeQuery(sqlPageId);
            resultPageId.next();
            statisticsDetailed.setPages(resultPageId.getInt("count(*)"));
            String sqlLemmaId = "SELECT COUNT(*) FROM lemma WHERE site_id=" +
                    statisticsDetailed.getId();
            ResultSet resultLemmaId = connection.createStatement().executeQuery(sqlLemmaId);
            resultLemmaId.next();
            statisticsDetailed.setLemmas(resultLemmaId.getInt("count(*)"));
            statisticsDetaileds.add(statisticsDetailed);
        }
        resultSiteDetailed.close();
        return statisticsDetaileds;
    }

    public static void saveName(String name, String url) throws SQLException {
        String sqlSiteName = "UPDATE site SET name='" + name + "' WHERE url='" + url + "'";
        connection.createStatement().executeUpdate(sqlSiteName);
        connection.createStatement().close();
    }

    public static void saveStatus(String status, String url, String error) throws SQLException {
        if (error.equals("")) {
            String sqlSiteName = "UPDATE site SET status='" + status + "' WHERE url='" + url + "'";
            connection.createStatement().executeUpdate(sqlSiteName);
            connection.createStatement().close();
        } else {
            String sqlSiteName = "UPDATE site SET status='" + status + "', last_error='" + error + "' WHERE url='" + url + "'";
            connection.createStatement().executeUpdate(sqlSiteName);
            connection.createStatement().close();
        }
    }

    public static String getSiteRoot(String url) throws SQLException {
        String sqlSiteRoot = "SELECT url FROM site";
        String rootSite = "";
        ResultSet resultSiteRoot = connection.createStatement().executeQuery(sqlSiteRoot);
        connection.createStatement().close();
        while (resultSiteRoot.next()) {
            rootSite = resultSiteRoot.getString("url");
            if (url.contains(rootSite)) {
                return rootSite;
            }
        }
        resultSiteRoot.close();
        return "";
    }

    public static String getStatus(String url) throws SQLException {
        String sqlSiteStatus = "SELECT status FROM site WHERE url='" + url + "'";
        ResultSet resultSiteStatus = connection.createStatement().executeQuery(sqlSiteStatus);
        connection.createStatement().close();
        resultSiteStatus.next();
        return resultSiteStatus.getString("status");
    }

    public static boolean getTestParsingSite(String name) throws SQLException {
        boolean testParsingSite = false;
        String sqlTestPageId = "SELECT COUNT(*) FROM page WHERE site_id=" +
                getSiteIdSearch(name);
        ResultSet resultTestPageId = connection.createStatement().executeQuery(sqlTestPageId);
        connection.createStatement().close();
        resultTestPageId.next();
        int a = resultTestPageId.getInt("count(*)");
        resultTestPageId.close();
        String sqlTestPageId1 = "SELECT code FROM page WHERE site_id=" +
                getSiteIdSearch(name) + " AND path='/'";
        ResultSet resultTestPageId1 = connection.createStatement().executeQuery(sqlTestPageId1);
        if (resultTestPageId1.next()) {
            int b = resultTestPageId1.getInt("code");
            if (a > 1) {
                if (b == 200) {
                    testParsingSite = true;
                }
            } else {
                testParsingSite = false;
                saveStatus("FAILED", name, "Ошибка индексации: главная страница сайта не доступна");
            }
        } else {
            testParsingSite = false;
            saveStatus("FAILED", name, "Ошибка индексации: главная страница сайта не доступна");
        }
        connection.createStatement().close();
        resultTestPageId1.close();
        return testParsingSite;
    }

    public static Set<String> searchUrlSql(Set<String> linkList, int siteId) throws SQLException {
        Set<String> linkList2 = new HashSet<>(linkList);
        if (!isIndexing.get() || linkList.size() == 0) {
            return linkList2;
        }
        String urlLinkList = "";
        for (String list : linkList) {
            urlLinkList = urlLinkList + "path = '" + list + "' OR ";
        }
        urlLinkList = urlLinkList.substring(0, urlLinkList.length() - 4);
        String searchUrlSql = "SELECT path FROM page WHERE (" + urlLinkList + ") AND site_id = " + siteId;
        ResultSet resultSearchUrlSql = connection.createStatement().executeQuery(searchUrlSql);
        while (resultSearchUrlSql.next()) {
            String url = resultSearchUrlSql.getString("path");
            for (String urlLink : linkList) {
                if (urlLink.equals(url)) {
                    linkList2.remove(url);
                    break;
                }
            }
        }
        connection.createStatement().close();
        resultSearchUrlSql.close();
        Set<String> linkList3 = new HashSet<>();
        for (String urlLink2 : linkList2) {
            if (!searchUrlSql1(urlLink2, siteId)) {
                linkList3.add(urlLink2);
            }
        }
        return linkList3;
    }

    public static boolean searchUrlSql1(String url, int siteId) throws SQLException {
        String searchUrlSql1 = "SELECT path FROM page WHERE path='" + url + "' AND site_id = " + siteId;
        ResultSet resultSearchUrlSql1 = connection.createStatement().executeQuery(searchUrlSql1);
        if (resultSearchUrlSql1.next()) {
            connection.createStatement().close();
            resultSearchUrlSql1.close();
            return true;
        }
        connection.createStatement().close();
        resultSearchUrlSql1.close();
        return false;
    }

    public static ResultSet searchOutputSqlSiteName(int siteId) throws SQLException {
        String searchOutputSqlSiteName = "";
        if (siteId > 0) {
            searchOutputSqlSiteName = "SELECT url, name FROM site WHERE id=" + siteId;
        }
        return connection.createStatement().executeQuery(searchOutputSqlSiteName);
    }

    public static boolean getIndexing(String url) throws SQLException {
        String sqlSiteIndexing = "";
        boolean statusIndexing = false;
        if (url.equals("")){
            sqlSiteIndexing = "SELECT status FROM site";
        } else {
            sqlSiteIndexing = "SELECT status FROM site WHERE url='" + url + "'";
        }
        ResultSet resultSiteIndexing = connection.createStatement().executeQuery(sqlSiteIndexing);
        connection.createStatement().close();
        while (resultSiteIndexing.next()) {
            String status1 = resultSiteIndexing.getString("status");
            if (status1.equals("INDEXED")) {
                statusIndexing = true;
            }
        }
        return statusIndexing;
    }

}
