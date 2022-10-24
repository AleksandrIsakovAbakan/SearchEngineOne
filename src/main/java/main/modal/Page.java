package main.modal;


public class Page {
    int lemmaId;
    int pageId;

    int siteIdSearch;
    float rankLemmaPage;

    public Page(Object[] args3) {
        this.lemmaId = (int) args3[0];
        this.pageId = (int) args3[1];
        this.rankLemmaPage = (float) args3[2];
        this.siteIdSearch = (int) args3[3];
    }

    public int getLemmaId() {
        return lemmaId;
    }

    public void setLemmaId(int lemmaId) {
        this.lemmaId = lemmaId;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public float getRankLemmaPage() {
        return rankLemmaPage;
    }

    public void setRankLemmaPage(float rankLemmaPage) {
        this.rankLemmaPage = rankLemmaPage;
    }

    public int getSiteIdSearch() {
        return siteIdSearch;
    }

    public void setSiteIdSearch(int siteIdSearch) {
        this.siteIdSearch = siteIdSearch;
    }
}
