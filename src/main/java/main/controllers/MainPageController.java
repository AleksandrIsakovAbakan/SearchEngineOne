package main.controllers;

import main.modal.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import java.sql.Connection;
import java.sql.SQLException;

@Controller
public class MainPageController {
    @Autowired
    SitesProperties sitesPropNew;
    @Autowired
    public Datasource datasource;
    Connection connection;

    @RequestMapping(value = {"{pathToWebInterface}", "/api{pathToWebInterface}"})
    public String index(Model model) throws SQLException {
        if (connection == null) {
            Connection connection = new GetConnection(datasource).getConnection();
            ConnectionMySql connectionMySql = new ConnectionMySql(connection);
        }
        return "index";
    }

}
