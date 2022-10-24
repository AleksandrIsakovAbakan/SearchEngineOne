package main.modal.statistics;

import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
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
