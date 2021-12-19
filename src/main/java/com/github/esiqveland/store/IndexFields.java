package com.github.esiqveland.store;

public class IndexFields {

    public static final class StringField extends IndexedField<String> {
        private StringField(String field, String value) {
            this.name = field;
            this.value = value;
        }

        public static StringField of(String field, String value) {
            return new StringField(field, value);
        }
    }
    public static final class LongField extends IndexedField<Long> {
        private LongField(String field, long value) {
            this.name = field;
            this.value = value;
        }

        public static LongField of(String field, long value) {
            return new LongField(field, value);
        }
    }
    public static final class IntField extends IndexedField<Integer> {
        private IntField(String field, int value) {
            this.name = field;
            this.value = value;
        }

        public static IntField of(String field, int value) {
            return new IntField(field, value);
        }
    }
    public static sealed abstract class IndexedField<T> permits StringField, IntField, LongField {
        protected String name;
        protected T value;

        public String getName() {
            return name;
        }

        public T getValue() {
            return value;
        }

        public static StringField of(String field, String value) {
            return StringField.of(field, value);
        }
        public static LongField of(String field, long value) {
            return LongField.of(field, value);
        }
        public static IntField of(String field, int value) {
            return IntField.of(field, value);
        }
    }

    IndexedField<String> name = IndexedField.of("name", "");
    IndexedField<String> content = IndexedField.of("content", "");
    IndexedField<Long> size = IndexedField.of("size", 0L);

}
