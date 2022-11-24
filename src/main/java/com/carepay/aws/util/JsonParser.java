package com.carepay.aws.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses JSON data into Java objects. Uses reflection to populate fields.
 */
public class JsonParser {

    /**
     * Parses JSON data into a Java object.
     *
     * @param is    Input stream containing JSON data.
     * @param clazz Class of the object to create.
     * @param <T>   Type of the object to create.
     * @return The parsed object.
     * @throws IOException If an I/O error occurs.
     */
    @SuppressWarnings("unchecked")
    public <T> T parse(final InputStream is, final Class<?> clazz) throws IOException {
        return (T) parseValue(new PushbackReader(new InputStreamReader(is)), clazz);
    }


    /**
     * Parses a JSON value.
     *
     * @param reader Reader to read from (supports pushback)
     * @param clazz  Class of the object to create.
     * @return The parsed object.
     * @throws IOException If an I/O error occurs.
     */
    protected Object parseValue(final PushbackReader reader, final Type clazz) throws IOException {
        try {
            int ch;
            while ((ch = reader.read()) > -1) {
                if (ch == '{') {
                    final Object obj = ((Class<?>) clazz).newInstance();
                    parseField(reader, obj);
                    return obj;
                } else if (ch == '[') {
                    return parseArray(reader, clazz);
                } else if (Character.isDigit(ch)) {
                    reader.unread(ch);
                    return parseNumber(reader, typeToClass(clazz));
                } else if (ch == '"') {
                    return parseStringValue(reader, clazz);
                } else if (ch == 't') {
                    return parseTrue(reader);
                } else if (ch == 'f') {
                    return parseFalse(reader);
                } else if (ch == 'n') {
                    return parseNull(reader);
                } else if (!Character.isWhitespace(ch)) {
                    throw new UnexpectedCharacterException(ch);
                }
            }
            throw new UnexpectedEndOfStreamException();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IOException(e);
        }
    }

    /**
     * Parse a JSON String value. It assumes that the first quote character has already been read.
     *
     * @param reader Reader to read from (supports pushback)
     * @param clazz  Class of the object to create.
     * @return The parsed string
     * @throws IOException If an I/O error occurs.
     */
    private Comparable<? extends Comparable<?>> parseStringValue(PushbackReader reader, Type clazz) throws IOException {
        final String str = parseString(reader);
        if (clazz instanceof Class && Instant.class.isAssignableFrom((Class<?>) clazz)) { // special case for Instant
            return Instant.parse(str);
        } else {
            return str;
        }
    }

    /**
     * Converts a type to a class, supporting generic types.
     *
     * @param type Type to convert.
     * @return The Java class
     */
    private Class<?> typeToClass(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    /**
     * Parses a JSON field from an object. It assumes the opening curly bracket has already been
     * read.
     *
     * @param reader Reader to read from (supports pushback)
     * @param obj    Object to populate.
     * @throws IOException If an I/O error occurs.
     */
    public void parseField(final PushbackReader reader, final Object obj) throws IOException {
        int ch;
        while ((ch = reader.read()) > -1) {
            if (ch == '"') {
                final String name = parseString(reader);
                final Optional<Field> field = ClassUtils.findField(obj.getClass(), name);
                while ((ch = reader.read()) > -1) {
                    if (ch == ':') {
                        @SuppressWarnings("unchecked") final Object value = parseValue(reader, field.map(Field::getGenericType).orElse(Object.class));
                        field.ifPresent(f -> {
                            try {
                                f.setAccessible(true);
                                f.set(obj, value);
                            } catch (IllegalAccessException e) {
                                throw new IllegalStateException(e);
                            }
                        });
                        break;
                    } else if (!Character.isWhitespace(ch)) {
                        throw new UnexpectedCharacterException(ch);
                    }
                }
            } else if (ch == '}') {
                return;
            } else if (ch != ',' && !Character.isWhitespace(ch)) {
                throw new UnexpectedCharacterException(ch);
            }
        }
    }

    /**
     * Parses a JSON array. It assumes the opening square bracket has already been read.
     *
     * @param reader          Reader to read from (supports pushback)
     * @param collectionClass Class of the collection to create.
     * @return The parsed collection.
     */
    @SuppressWarnings("unchecked")
    public List<Object> parseArray(final PushbackReader reader, final Type collectionClass) throws IOException {
        @SuppressWarnings("rawtypes") final Class<?> clazz = (Class<?>) ((ParameterizedType) collectionClass).getActualTypeArguments()[0];
        final List<Object> list = new ArrayList<>();
        int ch;
        while ((ch = reader.read()) > -1) {
            if (!Character.isWhitespace(ch)) {
                if (ch == ']') {
                    return list;
                } else if (ch != ',') {
                    reader.unread(ch);
                    list.add(parseValue(reader, clazz));
                }
            }
        }
        throw new UnexpectedEndOfStreamException();
    }

    /**
     * Parses a JSON string. It assumes the opening quote character has already been read.
     *
     * @param reader Reader to read from (supports pushback)
     * @return The parsed string.
     * @throws IOException If an I/O error occurs.
     */
    private String parseString(final PushbackReader reader) throws IOException {
        int ch;
        final StringBuilder sb = new StringBuilder();
        while ((ch = reader.read()) > -1) {
            if (ch == '"') {
                return sb.toString();
            } else if (ch == '\\') {
                sb.append((char) reader.read());
            } else {
                sb.append((char) ch);
            }
        }
        throw new UnexpectedEndOfStreamException();
    }

    /**
     * Parses a JSON 'true' boolean. It assumes the 't' character has already been read.
     *
     * @param reader Reader to read from (supports pushback)
     * @return The parsed boolean, will always be true
     * @throws IOException                  If an I/O error occurs.
     * @throws UnexpectedCharacterException If the stream does not contain 'rue'
     */
    private Object parseTrue(PushbackReader reader) throws IOException {
        int ch;
        if ((ch = reader.read()) != 'r' || (ch = reader.read()) != 'u' || (ch = reader.read()) != 'e') {
            throw new UnexpectedCharacterException(ch);
        }
        return Boolean.TRUE;
    }

    /**
     * Parses a JSON 'false' boolean. It assumes the 'f' character has already been read.
     *
     * @param reader Reader to read from (supports pushback)
     * @return The parsed boolean, will always be false
     * @throws IOException                  If an I/O error occurs.
     * @throws UnexpectedCharacterException If the stream does not contain 'alse'
     */
    private boolean parseFalse(PushbackReader reader) throws IOException {
        int ch;
        if ((ch = reader.read()) != 'a' || (ch = reader.read()) != 'l' || (ch = reader.read()) != 's' || (ch = reader.read()) != 'e') {
            throw new UnexpectedCharacterException(ch);
        }
        return Boolean.FALSE;
    }

    /**
     * Parses a JSON number (integer or floating point)
     *
     * @param reader Reader to read from (supports pushback)
     * @param clazz  Class of the number to create.
     * @return The parsed number.
     * @throws IOException If an I/O error occurs.
     */
    private Object parseNumber(PushbackReader reader, Class<?> clazz) throws IOException {
        final StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = reader.read()) > -1) {
            if (Character.isDigit(ch) || ch == '.') { // TODO: scientific, negative, treat floats different
                sb.append((char) ch);
            } else {
                reader.unread(ch);
                break;
            }
        }
        if (clazz == long.class || Long.class.isAssignableFrom(clazz)) {
            return Long.parseLong(sb.toString());
        } else if (clazz == double.class || Double.class.isAssignableFrom(clazz)) {
            return Double.parseDouble(sb.toString());
        } else if (clazz == float.class || Float.class.isAssignableFrom(clazz)) {
            return Float.parseFloat(sb.toString());
        } else if (Instant.class.isAssignableFrom(clazz)) {
            return Instant.ofEpochMilli(Long.parseLong(sb.toString()));
        } else {
            return Integer.parseInt(sb.toString());
        }
    }

    /**
     * Parses a JSON null. It assumes the 'n' character has already been read.
     *
     * @param reader Reader to read from (supports pushback)
     * @return The parsed null.
     * @throws IOException If an I/O error occurs.
     */
    private Object parseNull(PushbackReader reader) throws IOException {
        int ch;
        if ((ch = reader.read()) != 'u' || (ch = reader.read()) != 'l' || (ch = reader.read()) != 'l') {
            throw new UnexpectedCharacterException(ch);
        }
        return null;
    }

    /**
     * Exception throws in case of an unexpected character.
     */
    static class UnexpectedCharacterException extends IOException {
        private static final long serialVersionUID = 1L;

        public UnexpectedCharacterException(int ch) {
            super("Unexpected character: " + (char) ch);
        }
    }

    /**
     * Exception throws in case of an unexpected end of stream.
     */
    static class UnexpectedEndOfStreamException extends IOException {
        private static final long serialVersionUID = 1L;

        public UnexpectedEndOfStreamException() {
            super("Unexpected end of stream");
        }
    }
}