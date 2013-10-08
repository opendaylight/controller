/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.xml;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class XMLNetconfUtil {

    public static XPathExpression compileXPath(String xPath) {
        final XPathFactory xPathfactory = XPathFactory.newInstance();
        final XPath xpath = xPathfactory.newXPath();
        xpath.setNamespaceContext(new HardcodedNamespaceResolver("netconf",
                XmlNetconfConstants.RFC4741_TARGET_NAMESPACE));
        try {
            return xpath.compile(xPath);
        } catch (final XPathExpressionException e) {
            throw new IllegalStateException("Error while compiling xpath expression " + xPath, e);
        }
    }

}
