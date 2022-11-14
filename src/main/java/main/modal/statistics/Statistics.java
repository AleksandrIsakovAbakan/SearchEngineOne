package main.modal.statistics;


import java.util.ArrayList;

public class Statistics {
    StatisticsTotal total;
    ArrayList<StatisticsDetailed> detailed;

    public StatisticsTotal getTotal() {
        return total;
    }

    public void setTotal(StatisticsTotal total) {
        this.total = total;
    }

    public ArrayList<StatisticsDetailed> getDetailed() {
        return detailed;
    }

    public void setDetailed(ArrayList<StatisticsDetailed> detailed) {
        this.detailed = detailed;
    }
}
