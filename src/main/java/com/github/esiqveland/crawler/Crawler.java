package com.github.esiqveland.crawler;

import io.reactivex.rxjava3.core.Flowable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

public interface Crawler {
    Flowable<IndexableObject> crawl() throws IOException;

    sealed interface IndexableObject permits IndexableFile, IndexableFolder {}

    EnumSet<? extends OpenOption> READ = EnumSet.of(StandardOpenOption.READ);

    record IndexableFile(
            //String id,
            String name,
            Path path,
            long size,
            Instant modifiedAt,
            Instant createdAt
    ) implements IndexableObject {
        public Optional<ReadableByteChannel> reader() throws IOException {
            try {
                return Optional.ofNullable(Files.newByteChannel(this.path, READ));
            } catch (FileNotFoundException e) {
                return Optional.empty();
            }
        }
    }

    record IndexableFolder(
            //String id,
            String name,
            String path
    ) implements IndexableObject {
    }


}
