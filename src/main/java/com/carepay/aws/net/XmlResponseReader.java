package com.carepay.aws.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.carepay.aws.util.BeanSAXContentHandler;
import org.xml.sax.SAXException;

public class XmlResponseReader<T> implements ResponseReader<T> {
    private static final SAXParser PARSER;

    static {
        try {
            PARSER = SAXParserFactory.newInstance().newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private final Class<T> clazz;

    public XmlResponseReader(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T read(HttpURLConnection uc) throws IOException {
        try {
            T result = clazz.newInstance();
            PARSER.parse(uc.getResponseCode() < 300 ? uc.getInputStream() : uc.getErrorStream(), new BeanSAXContentHandler(result));
            return result;
        } catch (InstantiationException | IllegalAccessException | SAXException e) {
            throw new IOException(e);
        }
    }
}
