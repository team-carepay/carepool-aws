package com.carepay.aws.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * Utility class for reading XML files with namespaces
 */
public class SimpleNamespaceContext implements NamespaceContext {
    private final Map<String, String> prefixes = new HashMap<>();
    private final Map<String, String> namespaces = new HashMap<>();

    public SimpleNamespaceContext(final String... mappings) {
        for (int i = 0; i < mappings.length; i += 2) {
            final String prefix = mappings[i];
            final String namespace = mappings[i + 1];
            prefixes.put(prefix, namespace);
            namespaces.put(namespace, prefix);
        }
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return prefixes.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return namespaces.get(namespaceURI);
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        throw new UnsupportedOperationException();
    }
}
