/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml;

import com.google.common.base.Preconditions;
import java.util.List;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.util.xml.XmlElement;

public class SimpleAttributeReadingStrategy extends AbstractAttributeReadingStrategy {
    public SimpleAttributeReadingStrategy(String nullableDefault) {
        super(nullableDefault);
    }

    @Override
    AttributeConfigElement readElementHook(List<XmlElement> configNodes) throws NetconfDocumentedException {
        XmlElement xmlElement = configNodes.get(0);
        Preconditions.checkState(configNodes.size() == 1, "This element should be present only once " + xmlElement
                + " but was " + configNodes.size());

        String textContent = readElementContent(xmlElement);
        return AttributeConfigElement.create(postprocessNullableDefault(getNullableDefault()),
                postprocessParsedValue(textContent));
    }

    protected String readElementContent(XmlElement xmlElement) throws NetconfDocumentedException {
        return xmlElement.getTextContent();
    }

    @Override
    protected Object postprocessNullableDefault(String nullableDefault) {
        return nullableDefault;
    }

    protected Object postprocessParsedValue(String textContent) {
        return textContent;
    }

}
