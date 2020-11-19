package com.carepay.aws.util;

import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class DomUtils {
    private DomUtils() {
        throw new IllegalStateException();
    }

    public static Predicate<Node> hasName(final String name) {
        return n -> Node.ELEMENT_NODE == n.getNodeType() && name.equals(n.getLocalName());
    }

    public static Stream<Node> asStream(final NodeList nodeList) {
        return IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item);
    }

    public static String getElement(final Element item, final String name) {
        return DomUtils.asStream(item.getChildNodes())
                .filter(DomUtils.hasName(name))
                .findFirst()
                .map(Node::getTextContent)
                .orElse(null);
    }
}
