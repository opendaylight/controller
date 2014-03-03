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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleAttributeReadingStrategy extends AbstractAttributeReadingStrategy {
    private static final Logger logger = LoggerFactory.getLogger(SimpleAttributeReadingStrategy.class);


    public SimpleAttributeReadingStrategy(String nullableDefault) {
        super(nullableDefault);
    }

    @Override
    AttributeConfigElement readElementHook(List<XmlElement> configNodes) throws NetconfDocumentedException {
        XmlElement xmlElement = configNodes.get(0);
        Preconditions.checkState(configNodes.size() == 1, "This element should be present only once " + xmlElement
                + " but was " + configNodes.size());

        String textContent = "";
        try{
            textContent = readElementContent(xmlElement);
        }catch(IllegalStateException | NullPointerException e) {
            // yuma sends <attribute /> for empty value instead of <attribute></attribute>
            logger.warn("Ignoring exception caused by failure to read text element", e);
        }

        if (null == textContent){
            throw new NetconfDocumentedException(String.format("This element should contain text %s", xmlElement),
                    NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.invalid_value,
                    NetconfDocumentedException.ErrorSeverity.error);
        }
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
