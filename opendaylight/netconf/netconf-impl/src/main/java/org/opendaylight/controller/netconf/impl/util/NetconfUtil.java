package org.opendaylight.controller.netconf.impl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

// TODO purge nulls
public class NetconfUtil {

    private static final Logger logger = LoggerFactory.getLogger(NetconfUtil.class);

    public static NetconfMessage createMessage(final File f) {
        try {
            return createMessage(new FileInputStream(f));
        } catch (final FileNotFoundException e) {
            logger.warn("File {} not found.", f, e);
        }
        return null;
    }

    public static NetconfMessage createMessage(final InputStream is) {
        Document doc = null;
        try {
            doc = XmlUtil.readXmlToDocument(is);
        } catch (final IOException e) {
            logger.warn("Error ocurred while parsing stream.", e);
        } catch (final SAXException e) {
            logger.warn("Error ocurred while final parsing stream.", e);
        }
        return (doc == null) ? null : new NetconfMessage(doc);
    }
}
