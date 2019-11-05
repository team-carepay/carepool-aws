package com.carepay.aws;

import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * Small utility to support namespaces in xpath
 */
public class SimpleNamespaceContext implements NamespaceContext {

    private final String uri;
    private final String prefix;

    public SimpleNamespaceContext(String uri, String prefix) {
        this.uri = uri;
        this.prefix = prefix;
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return this.prefix.equals(prefix) ? uri : XMLConstants.NULL_NS_URI;
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
