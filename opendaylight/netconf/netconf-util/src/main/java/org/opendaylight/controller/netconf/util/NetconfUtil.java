/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public final class NetconfUtil {

    private static final Logger logger = LoggerFactory.getLogger(NetconfUtil.class);

    private NetconfUtil() {}

    public static NetconfMessage createMessage(final File f) {
        Preconditions.checkNotNull(f, "File parameter was null");
        try {
            return createMessage(new FileInputStream(f));
        } catch (final FileNotFoundException e) {
            logger.warn("File {} not found.", f, e);
        }
        return null;
    }

    public static NetconfMessage createMessage(final InputStream is) {
        Preconditions.checkNotNull(is, "InputStream parameter was null");
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

    public static Document checkIsMessageOk(NetconfMessage responseMessage) {
        return checkIsMessageOk(responseMessage.getDocument());
    }

    public static Document checkIsMessageOk(Document response) {
        XmlElement element = XmlElement.fromDomDocument(response);
        Preconditions.checkState(element.getName().equals(XmlNetconfConstants.RPC_REPLY_KEY));
        element = element.getOnlyChildElement();
        if (element.getName().equals(XmlNetconfConstants.OK)) {
            return response;
        }
        logger.warn("Can not load last configuration. Operation failed.");
        throw new IllegalStateException("Can not load last configuration. Operation failed: "
                + XmlUtil.toString(response));
    }
}
