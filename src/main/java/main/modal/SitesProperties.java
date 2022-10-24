package main.modal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("")
@Data
public class SitesProperties {

    List<SitesPogo> sites;

    public List<SitesPogo> getSites() {
        return sites;
    }

    public void setSites(List<SitesPogo> sites) {
        this.sites = sites;
    }

    @Override
    public String toString() {
        return "SitesPropNew{" +
                "sites=" + sites +
                '}';
    }
}
