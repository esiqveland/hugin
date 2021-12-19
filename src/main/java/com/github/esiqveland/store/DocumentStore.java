package com.github.esiqveland.store;

import com.github.esiqveland.hugin.documentstore.v1.Documentstore.StoredDocument;
import com.github.esiqveland.text.Tokenizers.Token;
import com.github.esiqveland.types.Documents.DocumentWithTokens;
import com.google.protobuf.InvalidProtocolBufferException;
import io.vavr.CheckedFunction1;
import org.rocksdb.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DocumentStore implements AutoCloseable {
    private final RocksDB rocksDB;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public final static Options OPTIONS = new Options();

    public DocumentStore(RocksDB rocksDB) {
        this.rocksDB = rocksDB;
    }

    public sealed interface DocumentOperation permits InsertDocument {
    }

    public record DocMetadata(
            String title,
            Instant modifiedAt,
            Instant createdAt
    ) {
    }

    public record InsertDocument(
            String docId,
            String ownerId,
            byte[] checksum,
            byte[] docContent,
            DocMetadata meta
    ) implements DocumentOperation {
        public static StoredDocument parse(byte[] bytes) {
            try {
                return StoredDocument.parseFrom(bytes);
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public record GetDocumentRequest(String docId) {
    }

    public record LookupResult(
            String docId,
            StoredDocument doc
    ) {
    }

    public CompletableFuture<Optional<LookupResult>> Get(GetDocumentRequest req) {
        return CompletableFuture.supplyAsync(() -> {
            var key = req.docId().getBytes(UTF_8);

            try (var opts = new ReadOptions()) {
                var val = rocksDB.get(opts, key);

                return Optional.ofNullable(val)
                        .map(InsertDocument::parse)
                        .map(s -> new LookupResult(
                                req.docId(),
                                s
                        ));
            } catch (RocksDBException e) {
                throw new RuntimeException("Get id=" + req.docId(), e);
            }
        }, executor);
    }

//    public CompletableFuture<Void> insertSingle(InsertDocument d) {
//        return CompletableFuture.runAsync(() -> {
//            try {
//                insertSingleInner(d);
//            } catch (RocksDBException e) {
//                throw new RuntimeException("insertSingle doc=" + d, e);
//            }
//        });
//    }

    public CompletableFuture<Void> insertBatch(List<DocumentWithTokens> d) {
        return CompletableFuture.runAsync(() -> {
            var wOpts = new WriteOptions()
                    .setSync(false);
            try {
                makeBatch(d, writeBatch -> {
                    this.rocksDB.write(wOpts, writeBatch);
                    this.rocksDB.syncWal();
                    this.rocksDB.flush(new FlushOptions().setWaitForFlush(true));
                    this.rocksDB.flushWal(true);
                    return null;
                });
            } catch (Throwable e) {
                throw new RuntimeException("insertSingle doc=" + d, e);
            }
        });
    }

    <T> T makeBatch(DocumentWithTokens d, Function<WriteBatch, T> use) throws RocksDBException {
        try (var wb = new WriteBatch()) {
            writeToBatch(wb, d);
            return use.apply(wb);
        }
    }

    private void writeToBatch(WriteBatch wb, DocumentWithTokens d) throws RocksDBException {
        var doc = d.doc();
        var tokens = d.tokens();

        for (Token token : tokens) {
            var key = Token.createTokenKey(token, doc.namespaceId());
            wb.put(key.getBytes(UTF_8), doc.docId().getBytes(UTF_8));
        }
    }

    private <T> T makeBatch(List<DocumentWithTokens> d, CheckedFunction1<WriteBatch, T> use) throws Throwable {
        try (var wb = new WriteBatch()) {
            for (DocumentWithTokens doc : d) {
                writeToBatch(wb, doc);
            }
            return use.apply(wb);
        }
    }

    @Override
    public void close() {
        rocksDB.close();
    }
}
