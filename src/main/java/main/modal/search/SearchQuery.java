package main.modal.search;

import main.modal.ConnectionMySql;
import main.modal.Data;
import main.modal.LuceneMorph;
import main.modal.Page;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

@Service
public class SearchQuery {
    static ResultSearch resultSearch;
    private static final Logger log = LogManager.getLogger(SearchQuery.class);

    public SearchQuery() {
    }

    public ResultSearch searchQueryOffset(Object[] args5) throws SQLException {
        int offset = (int) args5[0];
        int limit = (int) args5[1];
        String query = (String) args5[2];
        String site = (String) args5[3];
        log.info("Получен поисковый запрос: " + query);
        Object[] args1 = {query, site, offset, limit};
        resultSearch = searchQueryNew(args1);
        ResultSearch resultSearch1 = resultSearch;
        ArrayList<Data> searchData1 = new ArrayList<>();
        searchData1.addAll(resultSearch.getData());
        searchData1.forEach(t -> {
            log.debug("Найдена страница: " + t.getSiteName() + t.getUri());
        });
        ArrayList<Data> searchDataOffset = new ArrayList<>();
        searchDataOffset = searchData1;
        resultSearch1.setData(searchDataOffset);
        return resultSearch1;
    }

    public ResultSearch searchQueryNew(Object[] args1) throws SQLException {
        String query = (String) args1[0];
        String site = (String) args1[1];
        int offset = (int) args1[2];
        int limit = (int) args1[3];
        HashMap searchString3 = null;
        resultSearch = new ResultSearch();
        int siteId = ConnectionMySql.getSiteIdSearch(site);
        try {
            searchString3 = LuceneMorph.luceneMorphProcessing(query);
        } catch (IOException e) {
            log.error(e);
            e.printStackTrace();
        }
        if (searchString3 != null) {
            HashMap<Integer, Float> searchStringAnswer = null;
            searchStringAnswer = createSearchQuery(searchString3, siteId);
            Object[] args2 = {searchStringAnswer, searchString3, siteId, offset, limit};
            ArrayList<Data> searchData = searchQueryNewData(args2);
            resultSearch.setResult("true");
            resultSearch.setCount(searchStringAnswer.size());
            resultSearch.setData(searchData);
            log.info("Ответ отправлен: " + query);
        }
        return resultSearch;
    }

    public ArrayList<Data> searchQueryNewData(Object[] args2) {
        HashMap<Integer, Float> searchStringAnswer = (HashMap<Integer, Float>) args2[0];
        HashMap searchString3 = (HashMap) args2[1];
        int siteId = (int) args2[2];
        int offset = (int) args2[3];
        int limit = (int) args2[4];
        boolean flag = searchStringAnswer.equals(null);
        ArrayList<Data> searchData = new ArrayList<>();
        if (!flag) {
            Set<Integer> key1 = searchStringAnswer.keySet();
            int countKey = 0;
            for (Integer key : key1) {
                if (countKey >= offset && countKey < offset + limit) {
                    Data searchDataNew = new Data();
                    try {
                        Object[] args3 = {key, searchStringAnswer.get(key).floatValue(), searchString3, siteId};
                        searchDataNew = responseOutput(args3);
                    } catch (SQLException e) {
                        log.error(e);
                        e.printStackTrace();
                    } catch (IOException e) {
                        log.error(e);
                        e.printStackTrace();
                    }
                    searchData.add(searchDataNew);
                }
                countKey = countKey + 1;
            }
        }
        return searchData;
    }

    public HashMap<Integer, Float> createSearchQuery(HashMap<String, Double> searchString3, int siteId)
                                                    throws SQLException {
        HashMap<Integer, Float> searchString4 = new HashMap<>();
        HashMap<Integer, Integer> siteIdLemmaHash = new HashMap<>();
        ResultSet resultSet1 = ConnectionMySql.lemmaSelect(searchString3, siteId);
        while (resultSet1.next()) {
            int lemmaId = resultSet1.getInt("id");
            int siteIdLemma = resultSet1.getInt("site_id");
            float frequency = resultSet1.getFloat("frequency");
            searchString4.put(lemmaId, frequency);
            siteIdLemmaHash.put(siteIdLemma, lemmaId);
        }
        HashMap<Integer, Float> searchStringAnswer = searchQueryProcessing(searchString4, siteId, searchString3);
        return searchStringAnswer;
    }

    public HashMap<Integer, Float> searchQueryProcessing(HashMap<Integer, Float> searchStringSql,
                                            int siteId, HashMap<String, Double> searchString3) throws SQLException {
        HashMap<Integer, Float> searchStringSql1;
        HashMap<Integer, Float> searchString7 = new HashMap<>();
        HashMap<Integer, Float> searchString8 = new HashMap<>();
        ArrayList<Page> page2;
        searchStringSql1 = searchStringSql.entrySet()
                .stream().sorted(Collections.reverseOrder(comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
        Set<Integer> key2 = searchStringSql1.keySet();
        String stringIndex = "";
        for (Integer key : key2) {
            stringIndex = stringIndex + "lemma_id=" + key + " OR ";
        }
        if (stringIndex.length() < 4) {
            return relevanceCalculation(searchString7, siteId);
        }
        page2 = ConnectionMySql.searchQueryProcessingSql(stringIndex.substring(0, stringIndex.length() - 4), siteId);
        searchString7 = searchQueryProcessing2(page2, searchString3);
        return relevanceCalculation(searchString7, siteId);
    }

    private HashMap<Integer, Float> searchQueryProcessing2(ArrayList<Page> page2, HashMap<String, Double> searchString3) {
        HashMap<Integer, Float> pageSetId = new HashMap<>();
        HashMap<Integer, Float> pageSetIdEnd = new HashMap<>();
        HashMap<Integer, ArrayList<Integer>> pageSetIdAll = new HashMap<>();
        HashMap<Integer, ArrayList<Integer>> pageSetId1 = new HashMap<>();
        for (int i = 0; i < page2.size(); i++) {
            ArrayList<Integer> arrayPage;
            boolean flag = false;
            if (pageSetIdAll.get(page2.get(i).getPageId()) == null) {
                arrayPage = new ArrayList<>();
            } else {
                arrayPage = pageSetIdAll.get(page2.get(i).getPageId());
                for (int p = 0; p < arrayPage.size(); p++) {
                    if (arrayPage.get(p).equals(page2.get(i).getLemmaId())) {
                        flag = true;
                    }
                }
            }
            if (!flag) {
                arrayPage.add(arrayPage.size(), page2.get(i).getLemmaId());
                pageSetIdAll.put(page2.get(i).getPageId(), arrayPage);
                pageSetId.put(page2.get(i).getPageId(), page2.get(i).getRankLemmaPage());
            }
        }
        Set<Integer> key = pageSetIdAll.keySet();
        for (Integer key1 : key) {
            ArrayList<Integer> arrayPage1 = pageSetIdAll.get(key1);
            if (arrayPage1.size() == searchString3.size()) {
                pageSetId1.put(key1, arrayPage1);
                pageSetIdEnd.put(key1, pageSetId.get(key1).floatValue());
            }
        }
        return pageSetIdEnd;
    }

    private HashMap<Integer, Float> relevanceCalculation(HashMap<Integer,
            Float> searchStringRelevance, int siteId) throws SQLException {
        HashMap<Integer, Float> relevanceCalculationRank = new HashMap<>();
        HashMap<Integer, Float> relevanceCalculationRankRelative = new HashMap<>();
        if (searchStringRelevance.isEmpty()) {
            return relevanceCalculationRankRelative;
        }
        relevanceCalculationRank = ConnectionMySql.relevanceCalculationSql(searchStringRelevance);
        Float relevanceMax = relevanceCalculationRank.values().stream().max((a, b) -> a.compareTo(b)).get();
        for (int key9 : relevanceCalculationRank.keySet()) {
            Float relativeRelevance = relevanceCalculationRank.get(key9).floatValue() / relevanceMax;
            relevanceCalculationRank.put(key9, relativeRelevance);
        }
        relevanceCalculationRankRelative = relevanceCalculationRank.entrySet()
                .stream().sorted(Collections.reverseOrder(comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
        return relevanceCalculationRankRelative;
    }

    public Data responseOutput(Object[] args3) throws SQLException, IOException {
        Integer key = (Integer) args3[0];
        Float v = (Float) args3[1];
        HashMap<String, Double> searchString3 = (HashMap<String, Double>) args3[2];
        int siteId = (int) args3[3];
        Data searchData1 = new Data();
        ResultSet resultOutputSqlNamePage = ConnectionMySql.responseOutputSql(key, siteId);
        resultOutputSqlNamePage.next();
        searchData1.setUri(resultOutputSqlNamePage.getString("path"));
        String responseContent = resultOutputSqlNamePage.getString("content");
        String[] responseContentRus = resultOutputSqlNamePage.getString("content")
                .replaceAll("[^;а-яёА-Я ]", "").replaceAll("[\\s]{2,}", " ")
                .replaceAll(";", " ")
                .trim().split("\\s+");
        if (siteId == 0) {
            siteId = resultOutputSqlNamePage.getInt("site_id");
        }
        searchData1.setTitle(responseContent.substring(responseContent.indexOf("<title>") + 7,
                responseContent.indexOf("</title>")));
        searchData1.setSnippet(responseSnippet(responseContentRus, searchString3));
        resultOutputSqlNamePage.close();
        ResultSet resultOutputSqlSiteName = ConnectionMySql.searchOutputSqlSiteName(siteId);
        if (resultOutputSqlSiteName.next()) {
            String url1 = resultOutputSqlSiteName.getString("url");
            if (url1.substring(url1.length() - 1).equals("/")){
                url1 = url1.substring(0, url1.length() - 1);
            }
            searchData1.setSite(url1);
            searchData1.setSiteName(resultOutputSqlSiteName.getString("name"));
        }
        resultOutputSqlSiteName.close();
        searchData1.setRelevance(v);
        return searchData1;
    }

    public String responseSnippet(String[] responseContentRus, HashMap<String, Double> searchString3)
                                                                            throws IOException {
        ArrayList<String> wordTest1 = new ArrayList<>(List.of(responseContentRus));
        HashMap<String, ArrayList<Integer>> searchForOccurrences;
        HashMap<Integer, String> searchForOccurrencesMin = new HashMap<>();
        HashMap<Integer, String> searchForOccurrencesMax = new HashMap<>();
        String word = "";
        searchForOccurrences = responseSnippetSearchForOccurrences(wordTest1, searchString3, false);
        if (searchForOccurrences.size() == 0){
            searchForOccurrences = responseSnippetSearchForOccurrences(wordTest1, searchString3, true);
        }
        ArrayList<Integer> search;
        Set<String> key = searchForOccurrences.keySet();
        for (String key1 : key) {
            search = searchForOccurrences.get(key1);
            searchForOccurrencesMin.put(search.size(), key1);
        }
        Set<Integer> key2 = searchForOccurrencesMin.keySet();
        for (int key3 : key2) {
            String a = searchForOccurrencesMin.get(key3);
            ArrayList<Integer> b = searchForOccurrences.get(a);
            searchForOccurrencesMax = responseSnippet2(wordTest1, searchString3, b);
            Set<Integer> key5 = searchForOccurrencesMax.keySet();
            for (int key6 : key5) {
                word = searchForOccurrencesMax.get(key6);
                if (key6 >= searchString3.size()) {
                    return word;
                }
            }
        }
        return word;
    }

    public HashMap<String, ArrayList<Integer>> responseSnippetSearchForOccurrences(ArrayList<String> wordTest1,
                                                    HashMap<String, Double> searchString3, boolean flagSearch)
                                                    throws IOException {
        HashMap<String, ArrayList<Integer>> searchForOccurrences = new HashMap<>();
        Set<String> key = searchString3.keySet();
        for (int i = 0; i < wordTest1.size(); i++) {
            for (String key1 : key) {
                String keyShort = "";
                if (key1.length() > 4) {
                    keyShort = key1.substring(0, key1.length() - 2);
                } else {
                    keyShort = key1;
                }
                Object[] args4 = {wordTest1.get(i), keyShort, searchForOccurrences, i, flagSearch};
                searchForOccurrences = searchOccurrences(args4);
            }
        }
        return searchForOccurrences;
    }

    private HashMap<String, ArrayList<Integer>> searchOccurrences(Object[] args4) throws IOException{
        String word = (String) args4[0];
        String key1 = (String) args4[1];
        HashMap<String, ArrayList<Integer>> searchForOccurrences= (HashMap<String, ArrayList<Integer>>) args4[2];
        int i = (Integer) args4[3];
        boolean flagSearch = (Boolean) args4[4];
        ArrayList<Integer> occurrences;
        boolean flag = false;
        String wordSearch = word.toLowerCase(Locale.ROOT);
        if (flagSearch){
            wordSearch = LuceneMorph.luceneMorphProcessing2(word.toLowerCase(Locale.ROOT));
        }
        if (wordSearch.contains(key1)) {
            if (searchForOccurrences.containsKey(key1)) {
                occurrences = searchForOccurrences.get(key1);
                for (int o = 0; o < occurrences.size(); o++) {
                    if (occurrences.get(o) == i) {
                        flag = true;
                    }
                }
                if (!flag) {
                    occurrences.add(i);
                    searchForOccurrences.put(key1, occurrences);
                }
            } else {
                occurrences = new ArrayList<>();
                occurrences.add(i);
                searchForOccurrences.put(key1, occurrences);
            }
        }
        return searchForOccurrences;
    }

    public HashMap<Integer, String> responseSnippet2(ArrayList<String> wordTest1, HashMap<String, Double> searchString3,
                                                     ArrayList<Integer> chainSearchAll) throws IOException {
        HashMap<Integer, String> searchForOccurrencesMax = new HashMap<>();
        Set<String> key = searchString3.keySet();
        for (int i = 0; i < chainSearchAll.size(); i++) {
            String word = "";
            int count = 0;
            int indexStart = chainSearchAll.get(i) - 5;
            if (indexStart < 0) {
                indexStart = 0;
            }
            int indexEnd = indexStart + 25;
            if (indexEnd >= wordTest1.size()) {
                indexEnd = wordTest1.size();
            }
            for (int c = indexStart; c < indexEnd; c++) {
                String lemma1 = LuceneMorph.luceneMorphProcessing2(wordTest1.get(c).toLowerCase(Locale.ROOT));
                boolean flag = false;
                for (String key3 : key) {
                    if (lemma1.equals(key3)) {
                        flag = true;
                        count = count + 1;
                        break;
                    }
                }
                if (flag) {
                    word = word + " <b>" + wordTest1.get(c) + "</b> ";
                } else {
                    word = word + " " + wordTest1.get(c) + " ";
                }
            }
            searchForOccurrencesMax.put(count, word);
        }
        return searchForOccurrencesMax;
    }
}
