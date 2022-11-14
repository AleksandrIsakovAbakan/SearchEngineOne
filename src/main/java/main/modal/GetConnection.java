package main.modal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class GetConnection {
    public java.sql.Connection connection;

    public float titleSql;

    public float bodySql;
    public final Datasource datasource;

    private static final Logger log = LogManager.getLogger(GetConnection.class);

    public GetConnection(Datasource datasource) {
        this.datasource = datasource;
    }


    public Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(datasource.getUrl() +
                        "?user=" + datasource.getUsername() + "&password=" + datasource.getPassword() +
                        "&autoReconnect=true&useSSL=false");
                int dataBase = datasource.getUrl().lastIndexOf("/") + 1;
                connection.createStatement().execute("USE " + datasource.getUrl().substring(dataBase));
                createTable(connection);
                String sqlTitleBody = "SELECT * FROM field";
                ResultSet resultSetTitleBody = connection.createStatement().executeQuery(sqlTitleBody);
                resultSetTitleBody.next();
                String titleName = resultSetTitleBody.getString("name");
                if (titleName.equals("title")) {
                    ConnectionMySql.titleSql = resultSetTitleBody.getFloat("weight");
                }
                resultSetTitleBody.next();
                String bodyName = resultSetTitleBody.getString("name");
                if (bodyName.equals("body")) {
                    ConnectionMySql.bodySql = resultSetTitleBody.getFloat("weight");
                }
            } catch (SQLException e) {
                log.error(e);
                e.printStackTrace();
            }
        }
        return connection;
    }

    public void createTable(java.sql.Connection connection) throws SQLException {
        connection.createStatement().execute("DROP TABLE IF EXISTS field");
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS field(" +
                "id INT NOT NULL AUTO_INCREMENT, name VARCHAR(255) NOT NULL, " +
                "selector VARCHAR(255) NOT NULL, weight FLOAT NOT NULL, PRIMARY KEY(id))");
        connection.createStatement().execute("INSERT INTO field(name, selector, weight) VALUES ('title'," +
                " 'title', 1.0), ('body', 'body', 0.8)");
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS page(" +
                "id INT NOT NULL AUTO_INCREMENT, path TEXT NOT NULL, " +
                "code INT NOT NULL, content MEDIUMTEXT NOT NULL, site_id INT NOT NULL, " +
                "sum_rank_lemma_page FLOAT NOT NULL, PRIMARY KEY(id), UNIQUE KEY path(path(100), site_id), " +
                "KEY(site_id))");
        connection.createStatement().execute("alter table page convert to character set utf8mb4" +
                " collate utf8mb4_unicode_ci");
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS lemma(" +
                "id INT NOT NULL AUTO_INCREMENT, lemma VARCHAR(255) NOT NULL, " +
                "frequency INT NOT NULL, site_id INT NOT NULL, PRIMARY KEY(id), UNIQUE KEY lemma(lemma(100), site_id)," +
                " KEY(site_id))");
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS index_lemma(" +
                "id INT NOT NULL AUTO_INCREMENT, page_id INT NOT NULL, " +
                "lemma_id INT NOT NULL, rank_lemma_page FLOAT NOT NULL, site_id INT NOT NULL," +
                "PRIMARY KEY(id), KEY(lemma_id, page_id), KEY(rank_lemma_page, site_id), KEY(site_id))");
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS site(" +
                "id INT NOT NULL AUTO_INCREMENT, status ENUM('INDEXING', 'INDEXED', 'FAILED') NOT NULL, " +
                "status_time DATETIME NOT NULL, last_error TEXT, url VARCHAR(255) NOT NULL, " +
                "name VARCHAR(255) NOT NULL, PRIMARY KEY(id), KEY(url))");
    }

}
