package com.github.esiqveland.crawler;

import io.reactivex.rxjava3.core.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

public class FileSystemCrawler implements Crawler {
    private final Logger log = LoggerFactory.getLogger(FileSystemCrawler.class);
    private final Path crawlPath;

    public FileSystemCrawler(Path crawlPath) {
        this.crawlPath = crawlPath;
    }

    @Override
    public Flowable<IndexableObject> crawl() throws IOException {
        return Flowable.using(
                        () -> Files.walk(crawlPath),
                        Flowable::fromStream,
                        Stream::close
                )
                .flatMap((Path item) -> {
                    var f = item.toFile();
                    if (f.isDirectory()) {
                        return Flowable.just(new IndexableFolder(
                                f.getName(),
                                item.toString()
                        ));
                    } else if (f.isFile() && !f.isHidden()) {
                        BasicFileAttributes attr;
                        try {
                            attr = Files.readAttributes(item, BasicFileAttributes.class);
                        } catch (IOException e) {
                            log.warn("error reading attributes for path={}. skip.", item, e);
                            return Flowable.empty();
                        }

                        return Flowable.just(new IndexableFile(
                                f.getName(),
                                item,
                                f.length(),
                                attr.lastModifiedTime().toInstant(),
                                attr.creationTime().toInstant()
                        ));
                    } else {
                        log.info("skipping unsupported path={}", item);
                        return Flowable.empty();
                    }
                }, 3);
    }

}
