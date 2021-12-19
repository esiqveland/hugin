package com.github.esiqveland.app;

import com.github.esiqveland.crawler.Crawler;
import com.github.esiqveland.crawler.Crawler.IndexableFile;
import com.github.esiqveland.crawler.Crawler.IndexableFolder;
import com.github.esiqveland.crawler.Crawler.IndexableObject;
import com.github.esiqveland.crawler.FileSystemCrawler;
import com.github.esiqveland.parsers.FileContentParser;
import com.github.esiqveland.utils.Utils;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.esiqveland.utils.Utils.timed;

public class CrawlingParser {
    private final Logger log = LoggerFactory.getLogger(CrawlingParser.class);
    public static final long yte = 1L;
    public static final long kbyte = 1024;
    public static final long megabyte = 1024 * kbyte;

    private final FileContentParser parser;
    private final Scheduler scheduler = Schedulers.io();

    public CrawlingParser(FileContentParser parser) {
        this.parser = parser;
    }

    public record CrawledItem(
            IndexableObject object,
            String content
    ) {
    }

    public Flowable<CrawledItem> runCrawl(Path root) {
        var crawler = new FileSystemCrawler(root);
        try {
            return crawler.crawl()
                    .parallel(2, 1)
                    .runOn(scheduler)
                    .map(item -> switch (item) {
                        case IndexableFolder p -> handleFolder(item, p);
                        case IndexableFile f -> handleFile(item, f);
                    })
                    .sequential();
        } catch (IOException e) {
            throw new RuntimeException("error crawling path=" + root, e);
        }
    }

    private CrawledItem handleFile(IndexableObject item, IndexableFile f) {
        if (f.size() > 10 * megabyte) {
            log.info("skipping too large file name={} size={}", f.name(), f.size());
            return new CrawledItem(item, expandNameToContent(f.name()));
        }
        try (var stream = TikaInputStream.get(f.path())) {
            var type = timed(
                    () -> parser.detect(f, stream),
                    duration -> {
                        if (duration.toMillis() > 10) {
                            log.info("mediatype detect took {}ms", duration.toMillis());
                        }
                    }
            );

            stream.reset();
            var content = timed(
                    () -> parser.parse(f, stream, type),
                    duration -> {
                        if (duration.toMillis() > 10) {
                            log.info("parser type={} took {}ms", type, duration.toMillis());
                        }
                    }
            );

            return new CrawledItem(item, content);
        } catch (TikaException parseError) {
            log.info("ignoring parse error for path={}", f.path(), parseError);
            return new CrawledItem(item, expandNameToContent(f.name()));
        } catch (Throwable e) {
            throw new RuntimeException("error on file=" + f.path().toString(), e);
        }
    }

    private String expandNameToContent(String itemName) {
        var parts = Arrays.stream(itemName.split("\\w"))
                .flatMap(part -> Arrays.stream(part.split("_")));

        var content = Stream.concat(Stream.of(itemName), parts)
                .collect(Collectors.joining(" "));

        return content;
    }

    private CrawledItem handleFolder(IndexableObject item, IndexableFolder p) {
        return new CrawledItem(item, expandNameToContent(p.name()));
    }
}
