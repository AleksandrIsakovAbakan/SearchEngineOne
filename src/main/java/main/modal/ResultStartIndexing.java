package main.modal;

import org.springframework.stereotype.Component;

@Component
public class ResultStartIndexing {
    String result;
    String error;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
