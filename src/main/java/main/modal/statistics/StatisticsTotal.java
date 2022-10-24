package main.modal.statistics;

import org.springframework.stereotype.Component;

@Component
public class StatisticsTotal {
    int sites;
    int pages;
    int lemmas;
    String isIndexing;

    public int getSites() {return sites;}

    public void setSites(int sites) {this.sites = sites;}

    public int getPages() {return pages;}

    public void setPages(int pages) {this.pages = pages;}

    public int getLemmas() {return lemmas;}

    public void setLemmas(int lemmas) {this.lemmas = lemmas;}

    public String getIsIndexing() {
        return isIndexing;
    }

    public void setIsIndexing(String isIndexing) {
        this.isIndexing = isIndexing;
    }

    @Override
    public String toString() {
        return "StatisticsTotal{" +
                "sites=" + sites +
                ", pages=" + pages +
                ", lemmas=" + lemmas +
                ", isIndexing=" + isIndexing +
                '}';
    }
}
