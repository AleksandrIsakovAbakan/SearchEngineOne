package main.modal.indexing;

import main.modal.ConnectionMySql;
import main.modal.LuceneMorph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicInteger;

import static main.controllers.PagesController.isIndexing;

class MapRecursiveTask extends RecursiveTask<Set<MapRecursiveTask>> {
    private String urlHome;
    private String root;
    int siteId = 0;
    private String userAgent;
    Set<String> link;

    private boolean pageAll;

    private static final Logger log = LogManager.getLogger(MapRecursiveTask.class);

    public MapRecursiveTask(Object[] args) {
        this.urlHome = (String) args[0];
        this.root = (String) args[1];
        this.siteId = (int) args[2];
        this.link = (Set<String>) args[3];
        this.userAgent = (String) args[4];
        this.pageAll = (boolean) args[5];
    }

    @Override
    protected synchronized Set<MapRecursiveTask> compute() {
        Set<String> linkLink = new HashSet<>(createLinkLink());
        Set<MapRecursiveTask> taskList = new HashSet<>();
        for (String set : linkLink) {
            synchronized (set) {
                if (set.equals(root + "404.php") || link.contains(set) || !isIndexing.get()) { continue;}
                if (!pageAll) {
                    if (!(root.substring(0, root.length() - 1) + set).contains(urlHome)) { continue;}
                }
                int statusCode = 0;
                try {
                    statusCode = urlHomeSave(set, root, siteId);
                } catch (SQLException | IOException e) {
                    log.error(e);
                    e.printStackTrace();
                    try {
                        ConnectionMySql.saveException(e.getMessage(), siteId);
                    } catch (SQLException ex) {
                        log.error(e);
                        ex.printStackTrace();
                    }
                }
                if (statusCode == 200) {
                    Object[] args = {root.substring(0, root.length() - 1) + set,
                            root, siteId, link, userAgent, pageAll};
                    MapRecursiveTask task = new MapRecursiveTask(args);
                    task.fork();
                    taskList.add(task);
                    link.add(set);
                }
            }
        }
        return taskList;
    }

    public Set<String> createLinkLink() {
        Set<String> linkLink = new HashSet<>();
        if (!isIndexing.get()) {
            return linkLink;
        }
        if (urlHome.length() > root.length() * 2) {
            if (urlHome.substring(root.length() - 1, root.length() * 2 - 1).equals(root)) {
                urlHome = urlHome.substring(root.length() - 1);
            }
        }
        try {
            urlHomeSave(urlHome, root, siteId);
        } catch (SQLException | IOException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
        try {
            linkLink = ConnectionMySql.searchUrlSql(createSubtask(urlHome, siteId), siteId);
        } catch (IOException | SQLException e) {
            log.error(e);
            e.printStackTrace();
            try {
                ConnectionMySql.saveException(e.getMessage(), siteId);
            } catch (SQLException ex) {
                log.error(e);
                ex.printStackTrace();
            }
        }
        return linkLink;
    }

    public int urlHomeSave(String urlHome, String root, int siteId) throws SQLException, IOException {
        Document doc = null;
        String urlHome1 = "";
        int statusCode = 0;
        if (urlHome.contains(root)) {
            urlHome1 = urlHome.substring(root.length() - 1);
        } else {
            urlHome1 = urlHome;
        }
        if (!urlHome1.substring(urlHome1.length() - 1).equals("/")) {
            urlHome1 = urlHome1 + "/";
        }
        Connection.Response response = responseJsoup(root.substring(0, root.length() - 1)
                + urlHome1);
        if (response != null) {
            if (response.statusCode() != 200) {
                Object[] args1 = {urlHome1, response.statusCode(), doc, siteId};
                ConnectionMySql.executeInsert(args1);
                ConnectionMySql.saveException(response.statusCode() + " " + response.statusMessage(), siteId);
                log.debug(response.statusCode() + " " + response.statusMessage() + " - "
                        + root.substring(0, root.length() - 1) + urlHome1);
                statusCode = response.statusCode();
            } else {
                String root1 = root;
                if (root.substring(root.length() - 1).equals("/")) {
                    root1 = root.substring(0, root.length() - 1);
                }
                if (!ConnectionMySql.searchUrlSql1(urlHome1, siteId)) {
                    doc = Jsoup.connect(root1 + urlHome1).maxBodySize(0)
                            .ignoreHttpErrors(true).timeout(25000).get();
                    Object[] args2 = {urlHome1, response.statusCode(), doc, siteId};
                    ConnectionMySql.executeInsert(args2);
                    LuceneMorph.luceneMorphSearch(doc.toString(), urlHome1, siteId);
                    log.debug("Indexed page: " + root1 + urlHome1);
                }
                statusCode = response.statusCode();
            }
        } else {
            Object[] args3 = {urlHome1, 0, doc, siteId};
            ConnectionMySql.executeInsert(args3);
            log.debug("ResponseJsoup = null " + root.substring(0, root.length() - 1) + urlHome1);
            ConnectionMySql.saveException("responseJsoup = null " + urlHome1, siteId);
            statusCode = 0;
        }
        return statusCode;
    }

    public synchronized Set createSubtask(String urlHome, int siteId) throws IOException, SQLException {
        Document doc = null;
        Set<String> linkList = new HashSet<>();
        List<MapRecursiveTask> taskList = new ArrayList<>();
        String urlHome1 = urlHome.substring(root.length() - 1);
        if (urlHome1 == root + "404.php") {
            return linkList;
        }
        String urlHome2 = urlHome1;
        if (!urlHome.substring(urlHome.length() - 1).equals("/")) {
            urlHome2 = urlHome1 + "/";
        }
        Connection.Response response2;
        response2 = responseJsoup(root.substring(0, root.length() - 1) + urlHome2);
        if (response2 != null) {
            if (response2.statusCode() != 200) {
                linkList.addAll(processingCodesOther200(urlHome2, siteId));
            } else {
                String root1 = root;
                if (root.substring(root.length() - 1).equals("/")) {
                    root1 = root.substring(0, root.length() - 1);
                }
                doc = Jsoup.connect(root1 + urlHome2).maxBodySize(0).ignoreHttpErrors(true)
                        .timeout(20000).get();
                linkList.addAll(documentProcessing(doc));
            }
        } else {
            log.debug("ResponseJsoup = null " + urlHome2);
            ConnectionMySql.saveException("responseJsoup = null " + urlHome2, siteId);
        }
        return ConnectionMySql.searchUrlSql(linkList, siteId);
    }

    private synchronized Connection.Response responseJsoup(String urlNew) throws SQLException {
        Connection.Response response = null;
        try {
            try {
                wait(100);
                response = Jsoup.connect(urlNew).userAgent(userAgent)
                        .timeout(20000).ignoreHttpErrors(true).execute();
                return response;
            } catch (ConnectException e) {
                log.error(e);
                e.getMessage();
                ConnectionMySql.saveException(e.getMessage() + " - " + e.getLocalizedMessage(), siteId);
            } catch (InterruptedException e) {
                log.error(e);
                throw new RuntimeException(e);
            }
        } catch (IOException | SQLException e) {
            log.error(e);
            e.getLocalizedMessage();
            ConnectionMySql.saveException(e.getMessage(), siteId);
        }
        return response;
    }

    private Set processingCodesOther200(String urlHomeCodesOther200, int siteId) throws IOException, SQLException {
        Set<String> linkList2 = new HashSet<>();
        Document doc2 = null;
        if (urlHomeCodesOther200.substring(urlHomeCodesOther200.length() - 1).equals("/")) {
            urlHomeCodesOther200 = urlHomeCodesOther200.substring(0, urlHomeCodesOther200.length() - 1);
        }
        Connection.Response response1 = responseJsoup(root.substring(0, root.length() - 1) + urlHomeCodesOther200);
        if (response1 != null) {
            if (response1.statusCode() == 200) {
                linkList2.add(urlHomeCodesOther200);
                doc2 = Jsoup.connect(root.substring(0, root.length() - 1) + urlHomeCodesOther200)
                        .ignoreHttpErrors(true).timeout(10000).maxBodySize(0).get();
                linkList2.addAll(documentProcessing(doc2));
            } else {
                linkList2.add(urlHomeCodesOther200);
                return linkList2;
            }
        }
        return linkList2;
    }

    private String searchForNonIncludedCharacters(String attr1) {
        if (attr1.indexOf("{") > 0 || attr1.indexOf("[") > 0 || attr1.indexOf("#") > 0
                || attr1.indexOf("%") > 0 || attr1.indexOf("?") > 0 || attr1.indexOf("=") > 0
                || attr1.indexOf("(") > 0) {
            ArrayList<Integer> count = new ArrayList<Integer>();
            count.add(attr1.indexOf("{"));
            count.add(attr1.indexOf("["));
            count.add(attr1.indexOf("#"));
            count.add(attr1.indexOf("%"));
            count.add(attr1.indexOf("?"));
            count.add(attr1.indexOf("="));
            count.add(attr1.indexOf("("));
            AtomicInteger count0 = new AtomicInteger(265);
            count.forEach(t -> {
                if (t > 0 && t < count0.get()) {
                    count0.set(t);
                }
            });
            attr1 = attr1.substring(0, count0.get() - 1);
            if (attr1.length() > 1) {
                if (attr1.substring(attr1.length() - 1).equals("/")) {
                    attr1 = attr1.substring(0, attr1.length() - 1);
                    attr1 = attr1.substring(0, attr1.lastIndexOf("/"));
                } else {
                    attr1 = attr1.substring(0, attr1.lastIndexOf("/"));
                }
            }
        }
        return attr1;
    }

    private Set handlingLinks(String attr) {
        Set<String> linkList1 = new HashSet<>();
        String attr1 = searchForNonIncludedCharacters(attr);
        String attr2 = attr1;
        int searchFileType = attr1.lastIndexOf(".");
        if (searchFileType > 0) {
            if (attr1.length() - searchFileType > 3) {
                int searchFileTypeSlash = attr1.lastIndexOf("/");
                if (searchFileTypeSlash >= 0) {
                    attr2 = attr1.substring(0, searchFileTypeSlash);
                }
                if (attr1.substring(attr1.length() - 5).equals(".html")) {
                    attr2 = attr1;
                }
            }
        }
        int searchSlash = attr2.indexOf("/");
        int searchSecondSlash = attr2.indexOf("/", searchSlash + 1);
        if (searchSlash == 0 && searchSecondSlash < 0) {
            linkList1.add(attr2);
        }
        if (searchSecondSlash > 0) {
            linkList1.addAll(processingLinksWhenSecondSlashGreaterZero(attr2));
        }
        return linkList1;
    }

    private Set documentProcessing(Document doc1) {
        Set<String> list1 = new HashSet<>();
        String query = "[href*='\']";
        Elements elements = null;
        elements = doc1.select(query);
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            String attr = element.attr("href");
            int searchSlash = attr.indexOf("/");
            int searchSlash1 = attr.indexOf("/", searchSlash + 1);
            int searchSlash2 = attr.indexOf("/", searchSlash1 + 1);
            if (searchSlash < 0) { continue;
            } else if (searchSlash == 0) {
                String root1 = root.substring(0, root.length() - 1);
                int root2 = root1.lastIndexOf("/");
                String root3 = root1.substring(root2);
                if (searchSlash2 > 0 && attr.substring(0, searchSlash2).contains(root3)) {
                    if (searchSlash1 > 1) {
                        list1.addAll(handlingLinks(attr.substring(searchSlash1)));
                    } else if (searchSlash1 == 1) {
                        list1.addAll(handlingLinks(attr.substring(searchSlash2)));
                    }
                } else {
                    list1.addAll(handlingLinks(attr));
                }
            } else {
                if (searchSlash > 0 && urlHome.length() >= searchSlash) {
                    if (attr.indexOf(urlHome.substring(searchSlash)) >= 0) {
                        list1.addAll(handlingLinks(attr.substring(searchSlash2)));
                    } else {
                        continue;
                    }
                }
            }
        }
        return list1;
    }

    private Set processingLinksWhenSecondSlashGreaterZero(String attr) {
        Set<String> list2 = new HashSet<>();
        int a = attr.indexOf("/");
        int b = 0;
        int c = 0;
        if (a == 0) {
            for (int i = 0; i < 12; i++) {
                if (i == 0) {
                    c = 1;
                }
                if (c < attr.length()) {
                    b = attr.indexOf("/", c);
                    if (b > 0) {
                        list2.add(attr.substring(0, b));
                    } else {
                        list2.add(attr);
                        return list2;
                    }
                } else {
                    return list2;
                }
                c = b + 1;
            }
        }
        return list2;
    }
}
