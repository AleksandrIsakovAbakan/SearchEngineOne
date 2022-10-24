package main.modal;

import org.springframework.stereotype.Component;

@Component
public class SitesPogo {
    String url;
    String name;
    String root;

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "SitesPogo{" +
                "url='" + url + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
