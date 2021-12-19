package com.github.esiqveland.text;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Tokenizers {

    public record Token(String value, List<Integer> positions) {
        public String toIndexKey(String namespaceId) {
            return createTokenKey(this, namespaceId);
        }
        public static String createTokenKey(Token t, String namespaceId) {
            return createTokenKey(t.value(), namespaceId);
        }
        public static String createTokenKey(String tokenValue, String namespaceId) {
            return namespaceId + "|" + tokenValue;
        }
    }

    public record TokenizeResponse(List<Token> tokens) {
    }

    public static class TextStemmer {
        // TODO: implement
        public List<Token> stemTokens(List<Token> tokens) {
            return tokens;
        }
    }


    public interface Tokenizer {
        List<Token> textToTokens(String body);
    }

    public static class SillyTokenizer implements Tokenizer {
        private static final Predicate<String> p = Pattern.compile("[^A-Za-z0-9]").asMatchPredicate();

        public static SillyTokenizer create() {
            return new SillyTokenizer();
        }

        @Override
        public List<Token> textToTokens(String body) {
            var list = Arrays.asList(body.split("\\w"));
            return list.stream()
                    .filter(str -> str.length() >= 3)
                    .map(String::trim)
                    .filter(str -> !str.isBlank())
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .distinct()
                    //.filter(p)
                    // TODO: implement recording positions from @body
                    .map(t -> new Token(t, List.of(0)))
                    .collect(Collectors.toList());
        }

    }
}