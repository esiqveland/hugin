package com.github.esiqveland.store;

import com.github.esiqveland.text.Tokenizers.Token;
import com.github.esiqveland.types.Documents.DocumentWithTokens;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.vavr.CheckedFunction1;
import org.jetbrains.annotations.NotNull;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SearchIndexStore implements AutoCloseable {
    private static final AtomicLong INSERTS_TOTAL = new AtomicLong();
    private static final AtomicLong GETS_TOTAL = new AtomicLong();
    private static final Logger log = LoggerFactory.getLogger(SearchIndexStore.class);

    private final RocksDB rocksDB;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor();

    public final static String DELIMITER = ",";
    public final static StringAppendOperator OPERATOR = new StringAppendOperator(DELIMITER);

    public final static Options OPTIONS = new Options()
            .setMergeOperator(OPERATOR)
            .setCreateIfMissing(true);

    public SearchIndexStore(RocksDB rocksDB) {
        this.rocksDB = rocksDB;
    }

    public static SearchIndexStore open(String path) throws RocksDBException {
        return new SearchIndexStore(RocksDB.open(OPTIONS, path));
    }

    public record SearchHit(
            String docId,
            String namespaceId,
            String token
    ) {
    }

    public record SearchHits(List<SearchHit> hits) {
    }

    public record SearchRequest(List<String> accessibleNamespaces, String query) {
    }

    public CompletableFuture<SearchHits> query(SearchRequest req) {
        return CompletableFuture.supplyAsync(() -> {
            List<SearchHit> hits = req.accessibleNamespaces.stream()
                    .flatMap(ns -> queryNs(ns, req.query()))
                    .collect(Collectors.toList());

            return new SearchHits(hits);
        }, executor);
    }

    private Stream<SearchHit> queryNs(String namespaceId, String query) {
        try (var opts = new ReadOptions()) {
            var key = Token.createTokenKey(query, namespaceId);

            var raw = this.rocksDB.get(
                    opts,
                    key.getBytes(UTF_8)
            );
            if (raw == null) {
                return Stream.empty();
            }
            var value = new String(raw, UTF_8);
            var parts = Arrays.stream(value.split(DELIMITER));

            return parts.map(docId -> new SearchHit(
                    docId,
                    namespaceId,
                    ""
            ));
        } catch (RocksDBException e) {
            throw new RuntimeException("[queryNs] error ns=" + namespaceId + " q=" + query, e);
        }
    }

    public Flowable<List<DocumentWithTokens>> insertBatch(List<DocumentWithTokens> d) {
        var start = System.nanoTime();

        return Flowable.create(emitter -> {
            var tokens = prepareBatch(d);
            log.info("insertBatch size={} token={} total={}", d.size(), tokens.size(), INSERTS_TOTAL.get());

            try (var wOpts = new WriteOptions()) {
                for (InsertionToken t : tokens) {
                    rocksDB.merge(wOpts, t.key, t.value);
                    INSERTS_TOTAL.incrementAndGet();
                }
                emitter.onNext(d);
                //return d;
            } catch (Throwable e) {
                emitter.onError(new RuntimeException("insertBatch doc=" + d, e));
            } finally {
                emitter.onComplete();
                var elapsed = System.nanoTime() - start;
                var elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsed);
                log.info("insertBatch size={} took {}ms", d.size(), elapsedMs);
            }
        }, BackpressureStrategy.BUFFER);
    }

    <T> T useWriteBatch(InsertionToken token, Function<WriteBatch, T> use) throws RocksDBException {
        try (var wb = new WriteBatch()) {
            writeToBatch(wb, token);
            return use.apply(wb);
        }
    }

    private void writeToBatch(WriteBatch wb, InsertionToken t) throws RocksDBException {
        wb.merge(t.key(), t.value());
    }

    public record InsertionToken(byte[] key, byte[] value) implements Comparable<InsertionToken> {
        public static List<InsertionToken> from(DocumentWithTokens d) {
            var doc = d.doc();
            var tokens = d.tokens();
            var ns = doc.namespaceId();

            int count = tokens.size();

            List<InsertionToken> inserts = new ArrayList<>(count);

            for (Token token : tokens) {
                var key = Token.createTokenKey(token, doc.namespaceId()).getBytes(UTF_8);
                var value = doc.docId().getBytes(UTF_8);

                inserts.add(new InsertionToken(key, value));
            }
            Collections.sort(inserts);
            return inserts;
        }

        @Override
        public int compareTo(@NotNull InsertionToken b) {
            var a = this;
            return Arrays.compare(a.key, b.key);
        }
    }

    public static List<InsertionToken> prepareBatch(List<DocumentWithTokens> d) {
        var tokens = d.stream()
                .flatMap((DocumentWithTokens d1) -> InsertionToken.from(d1).stream())
                .sorted()
                .toList();
        return tokens;
    }

    private <T> T useWriteBatch(List<InsertionToken> tokens, CheckedFunction1<WriteBatch, T> use) throws Throwable {
        try (var wb = new WriteBatch()) {
            for (var token : tokens) {
                writeToBatch(wb, token);
            }
            return use.apply(wb);
        }
    }


    @Override
    public void close() throws Exception {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        rocksDB.close();
    }
}
