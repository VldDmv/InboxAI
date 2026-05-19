package com.inboxai.fetch;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RssFetchServiceTest {

    private final RssFetchService service = new RssFetchService();

    @Test
    void parsesRssItemsWithGuidLinkAndStrippedText() throws Exception {
        String rss = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0"><channel>
                    <title>Test feed</title>
                    <link>https://example.com</link>
                    <description>Test</description>
                    <item>
                        <title>Item one</title>
                        <link>https://example.com/1</link>
                        <guid>urn:item:1</guid>
                        <description>&lt;p&gt;Hello   world&lt;/p&gt;</description>
                        <pubDate>Wed, 01 Jan 2026 12:00:00 GMT</pubDate>
                    </item>
                    <item>
                        <title>Item two</title>
                        <link>https://example.com/2</link>
                        <description>Plain text</description>
                    </item>
                </channel></rss>
                """;

        List<FetchedItem> items = service.parse(new StringReader(rss));

        assertThat(items).hasSize(2);

        FetchedItem first = items.get(0);
        assertThat(first.guid()).isEqualTo("urn:item:1");
        assertThat(first.title()).isEqualTo("Item one");
        assertThat(first.link()).isEqualTo("https://example.com/1");
        assertThat(first.contentHtml()).contains("<p>");
        assertThat(first.contentText()).isEqualTo("Hello world");
        assertThat(first.publishedAt()).isEqualTo(Instant.parse("2026-01-01T12:00:00Z"));

        FetchedItem second = items.get(1);
        assertThat(second.guid()).isEqualTo("https://example.com/2");
        assertThat(second.contentText()).isEqualTo("Plain text");
        assertThat(second.publishedAt()).isNull();
    }

    @Test
    void parsesAtomEntries() throws Exception {
        String atom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <feed xmlns="http://www.w3.org/2005/Atom">
                    <title>Atom feed</title>
                    <id>urn:feed:1</id>
                    <updated>2026-01-01T00:00:00Z</updated>
                    <entry>
                        <id>urn:entry:1</id>
                        <title>Atom item</title>
                        <link href="https://example.com/a1"/>
                        <updated>2026-02-01T08:00:00Z</updated>
                        <content type="html">&lt;p&gt;Atom body&lt;/p&gt;</content>
                    </entry>
                </feed>
                """;

        List<FetchedItem> items = service.parse(new StringReader(atom));

        assertThat(items).hasSize(1);
        assertThat(items.get(0).guid()).isEqualTo("urn:entry:1");
        assertThat(items.get(0).contentText()).isEqualTo("Atom body");
        assertThat(items.get(0).publishedAt()).isEqualTo(Instant.parse("2026-02-01T08:00:00Z"));
    }
}
