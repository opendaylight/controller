/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util;

import static org.opendaylight.yangtools.yang.common.SimpleDateFormatUtil.getRevisionFormat;

import com.google.common.base.Preconditions;
import java.text.ParseException;
import java.util.Date;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public final class NetconfUtil {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfUtil.class);

    private NetconfUtil() {}

    public static Document checkIsMessageOk(Document response) throws NetconfDocumentedException {
        XmlElement element = XmlElement.fromDomDocument(response);
        Preconditions.checkState(element.getName().equals(XmlNetconfConstants.RPC_REPLY_KEY));
        element = element.getOnlyChildElement();
        if (element.getName().equals(XmlNetconfConstants.OK)) {
            return response;
        }
        LOG.warn("Can not load last configuration. Operation failed.");
        throw new IllegalStateException("Can not load last configuration. Operation failed: "
                + XmlUtil.toString(response));
    }

    public static String writeDate(final Date date) {
        return getRevisionFormat().format(date);
    }

    public static Date readDate(final String s) throws ParseException {
        return getRevisionFormat().parse(s);
    }

    public static void checkType(final Object value, final Class<?> clazz) {
        Preconditions.checkArgument(clazz.isAssignableFrom(value.getClass()), "Unexpected type " + value.getClass()
                + " should be " + clazz + " of " + value);
    }
}
