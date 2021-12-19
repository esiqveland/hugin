package com.github.esiqveland.store;

import com.github.esiqveland.text.Tokenizers.Token;
import com.github.esiqveland.types.Documents.DocumentWithTokens;
import com.google.common.primitives.UnsignedBytes;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

public class IndexBuilderStore implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(IndexBuilderStore.class);

    private static final AtomicLong INSERTS_TOTAL = new AtomicLong();
    private static final AtomicLong GETS_TOTAL = new AtomicLong();

    private final SstFileWriter rocksDB;

    public final static String DELIMITER = ",";
    public final static StringAppendOperator OPERATOR = new StringAppendOperator(DELIMITER);

    public final static Options OPTIONS = new Options()
            .setMergeOperator(OPERATOR)
            .setCreateIfMissing(true);

    public IndexBuilderStore(SstFileWriter rocksDB) {
        this.rocksDB = rocksDB;
    }

    public static IndexBuilderStore open(String path) throws RocksDBException {
        var env = new EnvOptions();
        var w = new SstFileWriter(env, OPTIONS);
        w.open(path);
        return new IndexBuilderStore(w);
    }

    public Flowable<List<DocumentWithTokens>> insertBatch(List<DocumentWithTokens> d) {
        var start = System.nanoTime();

        return Flowable.create(emitter -> {
            var tokens = prepareBatch(d);
            log.info("insertBatch size={} token={} total={}", d.size(), tokens.size(), INSERTS_TOTAL.get());
            try {
                for (InsertionToken t : tokens) {
                    rocksDB.merge(t.key, t.value);
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
            var namespaceId = doc.namespaceId();

            int count = tokens.size();

            List<InsertionToken> inserts = new ArrayList<>(count);

            for (Token token : tokens) {
                var key = Token.createTokenKey(token, namespaceId).getBytes(UTF_8);
                var value = doc.docId().getBytes(UTF_8);

                inserts.add(new InsertionToken(key, value));
            }
            inserts.sort((a, b) -> UnsignedBytes.lexicographicalComparator().compare(a.key, b.key));
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
        rocksDB.close();
    }
}
