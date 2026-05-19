package com.inboxai.fetch;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class RssFetchService {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final String USER_AGENT = "InboxAI/0.1 (+https://github.com/VldDmv/InboxAI)";

    public List<FetchedItem> fetchFromUrl(String url) throws IOException, FeedException {
        URLConnection conn = URI.create(url).toURL().openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        try (XmlReader reader = new XmlReader(conn)) {
            return convert(new SyndFeedInput().build(reader));
        }
    }

    public List<FetchedItem> parse(Reader reader) throws FeedException {
        return convert(new SyndFeedInput().build(reader));
    }

    private List<FetchedItem> convert(SyndFeed feed) {
        List<FetchedItem> out = new ArrayList<>(feed.getEntries().size());
        for (SyndEntry entry : feed.getEntries()) {
            String guid = firstNonBlank(entry.getUri(), entry.getLink());
            if (guid == null) {
                continue;
            }
            String html = extractHtml(entry);
            String text = html == null ? null : stripHtml(html);
            out.add(new FetchedItem(
                    guid,
                    entry.getTitle(),
                    entry.getLink(),
                    html,
                    text,
                    toInstant(firstNonNull(entry.getPublishedDate(), entry.getUpdatedDate()))
            ));
        }
        return out;
    }

    private static String extractHtml(SyndEntry entry) {
        for (SyndContent c : entry.getContents()) {
            if (c.getValue() != null && !c.getValue().isBlank()) {
                return c.getValue();
            }
        }
        SyndContent desc = entry.getDescription();
        return desc != null ? desc.getValue() : null;
    }

    private static String stripHtml(String html) {
        String stripped = HTML_TAG.matcher(html).replaceAll(" ");
        return WHITESPACE.matcher(stripped).replaceAll(" ").trim();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    private static <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }

    private static Instant toInstant(Date d) {
        return d == null ? null : d.toInstant();
    }
}
