package main.modal;

import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@Service
public class LuceneMorph {

    static RussianLuceneMorphology luceneMorph;

    public LuceneMorph() {
    }

    public static void luceneMorphSearch(String str, String urlStr, int siteId) throws IOException, SQLException {
        if (luceneMorph == null) {
            luceneMorph = new RussianLuceneMorphology();
        }
        int countTitle1 = str.indexOf("<title>");
        int countTitle2 = str.indexOf("</", countTitle1 + 2);
        if (countTitle2 <= 0) {
            countTitle2 = str.indexOf("body");
        }
        String strTitle = str.substring(countTitle1 + 7, countTitle2);
        HashMap<String, Integer> titleMap = luceneMorphProcessing(strTitle);
        String strBody = str.substring(str.indexOf("body") + 5);
        HashMap<String, Integer> bodyMap = luceneMorphProcessing(strBody);
        if (titleMap.size() > 0 && bodyMap.size() > 0) {
            ConnectionMySql.executeInsertLemma(titleMap, bodyMap, siteId);
            Object[] args = {titleMap, bodyMap, urlStr, siteId};
            ConnectionMySql.executeInsertIndex(args);
        }
    }

    public static HashMap luceneMorphProcessing(String str1) throws IOException {
        if (luceneMorph == null) {
            luceneMorph = new RussianLuceneMorphology();
        }
        HashMap<String, Integer> text2 = new HashMap<>();
        String[] text1 = str1.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-яё\\s])", " ")
                .trim().split("\\s+");

        for (int i = 0; i < text1.length; i++) {
            if (text1[i].length() <= 0) {
                continue;
            }
            String morphInfo = String.valueOf(luceneMorph.getMorphInfo(text1[i]));
            if (morphInfo.contains("ПРЕДЛ") || morphInfo.contains("СОЮЗ") || morphInfo.contains("МЕЖД")
                    || morphInfo.contains("ЧАСТ")) {
                text1[i] = "";
            }
            if (text1[i] != "") {
                List<String> luceneWord = luceneMorph.getNormalForms(text1[i]);
                String word = luceneWord.get(0);
                if (text2.containsKey(word)) {
                    int vol = text2.get(word).intValue();
                    vol = vol + 1;
                    text2.replace(word, vol);
                } else {
                    text2.put(word, 1);
                }
            }
        }
        return text2;
    }

    public static String luceneMorphProcessing2(String wordTest) throws IOException {
        if (luceneMorph == null) {
            luceneMorph = new RussianLuceneMorphology();
        }
        String word = "";
        if (wordTest.contains(".") || wordTest.contains(",")) {
            return word;
        } else {
            String morphInfo = String.valueOf(luceneMorph.getMorphInfo(wordTest));
            if (morphInfo.contains("ПРЕДЛ") || morphInfo.contains("СОЮЗ") || morphInfo.contains("МЕЖД")
                    || morphInfo.contains("ЧАСТ")) {
                wordTest = "";
            }
            if (wordTest != "") {
                List<String> luceneWord = luceneMorph.getNormalForms(wordTest);
                word = luceneWord.get(0);
            }
        }
        return word;
    }
}
