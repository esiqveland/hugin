package com.github.esiqveland;

import com.github.esiqveland.app.CrawlingParser;
import com.github.esiqveland.crawler.Crawler.IndexableFile;
import com.github.esiqveland.crawler.Crawler.IndexableFolder;
import com.github.esiqveland.dbus.DbusSearchProvider;
import com.github.esiqveland.dbus.DbusService;
import com.github.esiqveland.parsers.FileContentParser;
import com.github.esiqveland.store.IndexBuilderStore;
import com.github.esiqveland.store.SearchIndexStore;
import com.github.esiqveland.store.SearchIndexStore.SearchHits;
import com.github.esiqveland.store.SearchIndexStore.SearchRequest;
import com.github.esiqveland.text.TextTokenizer;
import com.github.esiqveland.text.Tokenizers.Token;
import com.github.esiqveland.types.Documents.DocumentWithContent;
import com.github.esiqveland.types.Documents.DocumentWithTokens;
import com.github.esiqveland.types.Documents.IndexDocument;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.hash.Hashing.md5;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static <T extends RocksObject> void use(T o, Consumer<T> consumer) {
        try (o) {
            consumer.accept(o);
        }
    }

    public static <T extends RocksObject, R> R use(T o, Function<T, R> function) {
        try (o) {
            return function.apply(o);
        }
    }

    public static String randomOwner() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String randomNamespace() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String docId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        var crawlingParser = new CrawlingParser(new FileContentParser());

        Files.createDirectories(Paths.get("./mytemp/store"));
        Files.createDirectories(Paths.get("./mytemp/db"));
        Files.createDirectories(Paths.get("./mytemp/db2"));

        // a static method that loads the RocksDB C++ library.
        RocksDB.loadLibrary();

        // the Options class contains a set of configurable DB options
        // that determines the behaviour of the database.

        var username = System.getenv("USER");

        var now = Instant.now();

        // a factory method that returns a RocksDB instance
        try (
                var db = SearchIndexStore.open("mytemp/db");
                var indexWriter = IndexBuilderStore.open("mytemp/db2/" + now.getEpochSecond() + ".sst");
        ) {

            runMain(username, crawlingParser, indexWriter, db);
        } catch (Exception err) {
            log.error("err: {}", err.getMessage(), err);
        }

    }

    private static void runMain(
            String username,
            CrawlingParser crawlingParser,
            IndexBuilderStore indexWriter,
            SearchIndexStore db
    ) throws Exception {
        var textTokenizer = new TextTokenizer();
        List<String> namespaces = List.of(username);

        var owner1 = randomOwner();
//        var ns1 = randomNamespace();
//        var ns2 = "d7a91b35b59142c88ac423bc45d39e82";
        var ns1 = namespaces.get(0);
        var ns2 = namespaces.get(0);

        var doc1 = new IndexDocument(
                owner1,
                ns1,
                docId(),
                "file1.txt",
                URI.create("https://example.org").toString()
        );

        var doc2 = new IndexDocument(
                owner1,
                ns1,
                docId(),
                "file2.txt",
                URI.create("https://example.org").toString()
        );

        var doc3 = new IndexDocument(
                owner1,
                ns2,
                docId(),
                "file3.txt",
                URI.create("https://example.org").toString()
        );

        var doct1 = new DocumentWithTokens(
                doc1,
                List.of(new Token("pølse", List.of(1)))
        );
        var doct2 = new DocumentWithTokens(
                doc2,
                List.of(
                        new Token("saft", List.of(1)),
                        new Token("suse", List.of(5))
                )
        );
        var doct3 = new DocumentWithTokens(
                doc3,
                List.of(
                        new Token("løk", List.of(1)),
                        new Token("rødgrøt", List.of(7))
                )
        );

        List<DocumentWithTokens> batch1 = List.of(
                doct1,
                doct2,
                doct3
        );
        log.info("batch1={}", batch1);

        var crawlSource = crawlingParser
                .runCrawl(Path.of("./"))
                .map(item -> switch (item.object()) {
                    case IndexableFolder f -> {
                        var path = f.path();
                        var doc = new IndexDocument(
                                username,
                                ns1,
                                md5().hashString(path, UTF_8).toString(),
                                //f.id(),
                                f.name(),
                                path
                        );
                        yield new DocumentWithContent(doc, item.content());
                    }
                    case IndexableFile f -> {
                        var path = f.path();

                        var doc = new IndexDocument(
                                username,
                                ns1,
                                md5().hashString(path.toString(), UTF_8).toString(),
                                //f.id(),
                                f.name(),
                                path.toString()
                        );

                        yield new DocumentWithContent(doc, item.content());
                    }
                })
                .map(item -> textTokenizer.tokenize(item.doc(), item.content()))
                .buffer(100)
                .flatMap(batch -> db.insertBatch(batch)
                        .flatMap(ignored -> Flowable.fromIterable(batch).map(DocumentWithTokens::doc), 1), 1)
                .subscribeOn(Schedulers.newThread());

        var count = new AtomicLong();
//        var cancelCrawl = crawlSource.subscribe(
//                item -> {
//                    count.incrementAndGet();
//                    String name = item.docName();
//                    log.info("crawled item name={} uri={}", name, item.uri());
//                },
//                throwable -> {
//                    log.error("error: ", throwable);
//                },
//                () -> {
//                    log.info("crawl done count={}", count.get());
//                }
//        );

        var latch = new CountDownLatch(1);

        Signal.handle(
                new Signal("INT"),  // catches SIGINT
                signal -> {
                    log.warn("Interrupted by Ctrl+C");
                    latch.countDown();
                }
        );

        Runtime
                .getRuntime()
                .addShutdownHook(new Thread(() -> {
                    try {
                        Thread.sleep(200);
                        log.warn("Interrupted by shutdown hook");
                        latch.countDown();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        e.printStackTrace();
                    }
                }));

        var job1 = db.insertBatch(batch1).blockingLast();

        var hits1 = db.query(new SearchRequest(
                List.of(ns1, ns2),
                "løk"
        )).join();

        logHits(hits1);

        var sp2 = new DbusSearchProvider(db, namespaces);
        try (var dbus = new DbusService(sp2)) {
            latch.await();
//            if (!cancelCrawl.isDisposed()) {
//                cancelCrawl.dispose();
//            }
        } finally {
        }

    }

    private static void logHits(SearchHits hits1) {
        log.info("hits1={}", hits1);
    }
}