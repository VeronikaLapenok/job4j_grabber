package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse implements Parse {
    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String.format(
            "%s/vacancies/java_developer", SOURCE_LINK);
    private static final int COUNT_PAGE = 5;

    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    @Override
    public List<Post> list(String link) {
        List<Post> vacancyList = new ArrayList<>();
        for (int i = 1; i <= COUNT_PAGE ; i++) {
            Connection connection = Jsoup.connect(String.format("%s?page=%s", link, i));
            Document document = null;
            try {
                document = connection.get();
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
            Elements rows = document.select(".vacancy-card__inner");
            for (Element row : rows) {
                vacancyList.add(parsePost(row));
            }
        }
        return vacancyList;
    }

    private Post parsePost(Element row) {
        Element titleElement = row.select(".vacancy-card__title").first();
        Element linkElement = titleElement.child(0);
        Element dataElement = row.select(".vacancy-card__date").first().child(0);
        String vacancyName = titleElement.text();
        String date = dataElement.attr("datetime");
        String vacancyLink = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
        String description = retrieveDescription(vacancyLink);
        return new Post(vacancyName, vacancyLink, description, dateTimeParser.parse(date));
    }

    private String retrieveDescription(String link) {
        Connection connection = Jsoup.connect(link);
        try {
            Document document = connection.get();
            Elements rows = document.select(".style-ugc");
            return rows.text();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
