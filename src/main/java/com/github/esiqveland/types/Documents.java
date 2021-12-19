package com.github.esiqveland.types;

import com.github.esiqveland.text.Tokenizers;

import java.net.URI;
import java.util.List;

public class Documents {

    public record DocumentWithTokens(
            IndexDocument doc,
            List<Tokenizers.Token> tokens
    ) {}

    public record DocumentWithContent(
            IndexDocument doc,
            String content
    ) {}

    public record IndexDocument(
            String owner,
            String namespaceId,
            String docId,
            String docName,
            String uri
    ) {}

}
