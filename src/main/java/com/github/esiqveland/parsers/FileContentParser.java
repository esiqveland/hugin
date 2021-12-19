package com.github.esiqveland.parsers;

import com.github.esiqveland.crawler.Crawler.IndexableFile;
import org.apache.poi.EmptyFileException;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.DefaultParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;

//See: https://tika.apache.org/2.1.0/parser.html
public class FileContentParser {
    public final Detector detector = new DefaultDetector();
    public final Parser parser = new DefaultParser();
    public final Parser parser2 = new AutoDetectParser();

    public MediaType detect(IndexableFile f, TikaInputStream inputStream) throws IOException {
        var meta = new Metadata();
        // The name of the file or resource that contains the document.
        meta.set(TikaCoreProperties.RESOURCE_NAME_KEY, f.name());
        meta.set(Metadata.CONTENT_LENGTH, Long.toString(f.size()));

        return detector.detect(inputStream, meta);
    }

    public String parse(IndexableFile f, TikaInputStream inputStream, MediaType mediaType) throws IOException, TikaException {
        var meta = new Metadata();
        meta.set(TikaCoreProperties.RESOURCE_NAME_KEY, f.name());
        //meta.set(Metadata.TITLE, );
        //meta.set(Metadata.Metadata.AUTHOR, );
        meta.set(TikaCoreProperties.CONTENT_TYPE_HINT, mediaType.toString());
        meta.set(Metadata.CONTENT_TYPE, mediaType.toString());
        meta.set(Metadata.CONTENT_LENGTH, Long.toString(f.size()));

        BodyContentHandler handler = new BodyContentHandler();
        try {
            if (f.size() == 0) {
                return f.name();
            }
            ParseContext ctx = new ParseContext();

            parser.parse(inputStream, handler, meta, ctx);

            return handler.toString();
        } catch (WriteLimitReachedException e) {
            // document had too much text, just return what we have:
            return handler.toString();
        } catch (TikaException e) {
            if (e.getCause() instanceof EmptyFileException) {
                return f.name();
            } else {
                throw e;
            }
        } catch (SAXException e) {
            throw new RuntimeException("error parsing", e);
        }
    }
}
