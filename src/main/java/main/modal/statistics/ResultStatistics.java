package main.modal.statistics;

import org.springframework.stereotype.Component;

@Component
public class ResultStatistics {
    String result;
    Statistics statistics;
    public String getResult() {return result;}
    public void setResult(String result) {this.result = result;}
    public Statistics getStatistics() {return statistics;}
    public void setStatistics(Statistics statistics) {this.statistics = statistics;}
    }
