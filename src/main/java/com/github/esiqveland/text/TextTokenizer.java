package com.github.esiqveland.text;

import com.github.esiqveland.types.Documents;
import io.netty.util.internal.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TextTokenizer {
    private final Tokenizers.TextStemmer textStemmer = new Tokenizers.TextStemmer();
    private final Tokenizers.Tokenizer tokenizer = Tokenizers.SillyTokenizer.create();

    // TODO: implement tokenizer
    public Documents.DocumentWithTokens tokenize(Documents.IndexDocument doc, String textBody) {
        var stemmedTokens = textStemmer.stemTokens(tokenizer.textToTokens(textBody));
        return new Documents.DocumentWithTokens(doc, stemmedTokens);
    }

    public Documents.DocumentWithTokens tokenize(
            Documents.IndexDocument doc,
            Function<Documents.IndexDocument, Tokenizers.TokenizeResponse> docLoader
    ) {
        var tokens = docLoader.apply(doc);

        var tokenList = tokens.tokens();
        if (!StringUtil.isNullOrEmpty(doc.docName())) {
            tokenList = new ArrayList<>(tokens.tokens().size() + 1);
            tokenList.add(new Tokenizers.Token(doc.docName(), List.of(0)));
        }

        return new Documents.DocumentWithTokens(
                doc,
                tokenList
        );
    }
}
