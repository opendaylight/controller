/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.toxml;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Map;
import org.opendaylight.controller.config.facade.xml.util.Util;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SimpleIdentityRefAttributeWritingStrategy extends SimpleAttributeWritingStrategy {

    private static final String PREFIX = "prefix";

    /**
     * @param document
     * @param key
     */
    public SimpleIdentityRefAttributeWritingStrategy(Document document, String key) {
        super(document, key);
    }

    protected Object preprocess(Object value) {
        Util.checkType(value, Map.class);
        Preconditions.checkArgument(((Map<?, ?>)value).size() == 1, "Unexpected number of values in %s, expected 1", value);
        Object stringValue = ((Map<?, ?>) value).values().iterator().next();
        Util.checkType(stringValue, String.class);

        return stringValue;
    }

    @Override
    protected Element createElement(Document doc, String key, String value, Optional<String> namespace) {
        QName qName = QName.create(value);
        String identityValue = qName.getLocalName();
        String identityNamespace = qName.getNamespace().toString();
        return XmlUtil.createTextElementWithNamespacedContent(doc, key, PREFIX, identityNamespace, identityValue, namespace);
    }
}
