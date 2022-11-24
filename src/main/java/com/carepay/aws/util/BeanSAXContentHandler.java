package com.carepay.aws.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX Handler for Java Beans. Uses reflection to set fields.
 */
public class BeanSAXContentHandler extends DefaultHandler {

    /**
     * Marker for fields that are not set.
     */
    private static final Field NO_SUCH_FIELD;
    /**
     * Dummy object for unmapped fields.
     */
    private static final Object NO_SUCH_OBJECT = new Object();

    static {
        Field f = null;
        try {
            f = BeanSAXContentHandler.class.getDeclaredField("NO_SUCH_OBJECT");
        } catch (NoSuchFieldException e) {
            // should never happen
        }
        NO_SUCH_FIELD = f;
    }

    boolean rootElementFound;

    private final Deque<Object> stack = new ArrayDeque<>();
    private Field field;
    private final StringBuilder value = new StringBuilder();

    public BeanSAXContentHandler(final Object obj) {
        stack.push(obj);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        try {
            if (!rootElementFound) { // root element, skip
                rootElementFound = true;
                return;
            }
            if (field != null) {
                // nesting
                Object child = field.getType().newInstance();
                if (field != NO_SUCH_FIELD) {
                    field.set(stack.peek(), child);
                }
                stack.push(child);
            } else if (stack.peek() instanceof CollectionWrapper) {
                stack.push(((CollectionWrapper) stack.peek()).add());
                return; // ignore the start element for collections and arrays
            }
            findField(Character.toLowerCase(qName.charAt(0)) + qName.substring(1));
        } catch (IllegalAccessException | InstantiationException e) {
            throw new SAXException(e);
        }
    }

    private void findField(String fieldName) {
        Class<? extends Object> clazz = stack.peek().getClass();
        do {
            try {
                field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                if (Collection.class.isAssignableFrom(field.getType())) {
                    final Collection<Object> collection = (Collection<Object>) field.getType().newInstance();
                    field.set(stack.peek(), collection);
                    stack.push(new CollectionWrapper(collection, (Class<Object>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]));
                    field = null;
                }
                return;
            } catch (NoSuchFieldException | IllegalAccessException | InstantiationException e) {
                // not found, try superclass
                clazz = clazz.getSuperclass();
            }
        } while (clazz != null);
        field = NO_SUCH_FIELD;
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (field != null) {
            final String trimmedValue = value.toString().trim();
            if (field != NO_SUCH_FIELD && !trimmedValue.isEmpty()) {
                try {
                    if (field.getType().isAssignableFrom(String.class)) {
                        field.set(stack.peek(), trimmedValue);
                    } else if (field.getType().isAssignableFrom(Integer.class)) {
                        field.set(stack.peek(), Integer.valueOf(trimmedValue));
                    } else if (field.getType().isAssignableFrom(Instant.class)) {
                        field.set(stack.peek(), Instant.parse(trimmedValue));
                    } else {
                        throw new SAXException("unsupported field type: " + field.getType());
                    }
                } catch (IllegalAccessException e) {
                    throw new SAXException(e);
                }
            }
        } else {
            // nesting ends
            stack.pop();
        }
        field = null;
        value.setLength(0);
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        value.append(ch, start, length);
    }

    static class CollectionWrapper {
        private Collection<Object> collection;
        private Class clazz;

        public CollectionWrapper(Collection<Object> collection, Class clazz) {
            this.collection = collection;
            this.clazz = clazz;
        }

        public Object add() throws InstantiationException, IllegalAccessException {
            Object child = clazz.newInstance();
            collection.add(child);
            return child;
        }
    }
}