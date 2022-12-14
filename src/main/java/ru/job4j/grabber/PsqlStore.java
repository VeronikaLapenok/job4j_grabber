package ru.job4j.grabber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PsqlStore.class.getName());
    private Connection connection;

    public PsqlStore(Properties config) {
        try {
            Class.forName(config.getProperty("jdbc.driver"));
            connection = DriverManager.getConnection(
                    config.getProperty("jdbc.url"),
                    config.getProperty("jdbc.username"),
                    config.getProperty("jdbc.password")
            );
        } catch (Exception e) {
            LOG.error("Invalid connection");
        }
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        "insert into post (name, link, description, created) values (?, ?, ?, ?)" +
                                " on conflict (link) do nothing",
                        Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, post.getTitle());
            statement.setString(2, post.getLink());
            statement.setString(3, post.getDescription());
            statement.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            statement.execute();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    post.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException sqlException) {
            LOG.error("Post saving error");
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("select * from post")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    posts.add(createResultPost(resultSet));
                }
            }
        } catch (SQLException sqlException) {
            LOG.error("All posts getting error");
        }
        return posts;
    }

    @Override
    public Post findById(int id) {
        Post post = null;
        try (PreparedStatement statement =
                     connection.prepareStatement("select * from post where id = ?")) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                post = createResultPost(resultSet);
            }
        } catch (SQLException sqlException) {
            LOG.error("Finding post by ID error");
        }
        return post;
    }

    private static Post createResultPost (ResultSet resultSet) throws SQLException {
        return new Post(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("link"),
                resultSet.getString("description"),
                resultSet.getTimestamp("created").toLocalDateTime()
        );
    }

    private static Properties load() {
        Properties config = new Properties();
        try (InputStream in = PsqlStore.class.getClassLoader()
                .getResourceAsStream("post.properties")) {
            config.load(in);
        } catch (IOException e) {
            LOG.error("Loading properties error");
        }
        return config;
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    public static void main(String[] args) {
        try (PsqlStore psqlStore = new PsqlStore(load())) {
            Post post = new Post("Java ??????????????????????", "https://career.habr.com/vacancies/1000080748",
                    "???????????? ???????????????????? Sportmaster Lab ???????? ?? ???????? ?????????????? Java Developers",
                    LocalDateTime.now());
            psqlStore.save(post);
            System.out.println(psqlStore.findById(1));
        } catch (SQLException sqlException) {
            LOG.error("Demonstrating methods error");
        }
    }
}
