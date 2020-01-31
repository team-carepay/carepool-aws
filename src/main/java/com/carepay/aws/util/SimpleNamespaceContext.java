package com.carepay.aws.util;

import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * Utility class for reading XML files with namespaces
 */
public class SimpleNamespaceContext implements NamespaceContext {
    private final String prefix;
    private final String namespace;

    public SimpleNamespaceContext(final String prefix, final String namespace) {
        this.prefix = prefix;
        this.namespace = namespace;
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return this.prefix.equals(prefix) ? namespace : XMLConstants.NULL_NS_URI;
    }

    @Override
    public String getPrefix(String namespaceURI) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        throw new UnsupportedOperationException();
    }
}
