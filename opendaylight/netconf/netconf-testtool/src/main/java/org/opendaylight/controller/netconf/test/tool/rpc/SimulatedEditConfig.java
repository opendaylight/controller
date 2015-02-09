/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool.rpc;

import com.google.common.base.Optional;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.AbstractConfigNetconfOperation;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfigXmlParser;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SimulatedEditConfig extends AbstractConfigNetconfOperation {
    private static final String DELETE_EDIT_CONFIG = "delete";
    private static final String OPERATION = "operation";
    private static final String REMOVE_EDIT_CONFIG = "remove";
    private final DataList storage;

    public SimulatedEditConfig(final String netconfSessionIdForReporting, final DataList storage) {
        super(null, netconfSessionIdForReporting);
        this.storage = storage;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(final Document document, final XmlElement operationElement) throws NetconfDocumentedException {
        final XmlElement configElementData = operationElement.getOnlyChildElement(XmlNetconfConstants.CONFIG_KEY);

        containsDelete(configElementData);
        if(containsDelete(configElementData)){
            storage.resetConfigList();
        } else {
            storage.setConfigList(configElementData.getChildElements());
        }

        return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
    }

    @Override
    protected String getOperationName() {
        return EditConfigXmlParser.EDIT_CONFIG;
    }

    private boolean containsDelete(final XmlElement element) {
        for (final Attr o : element.getAttributes().values()) {
            if (o.getLocalName().equals(OPERATION)
                    && (o.getValue().equals(DELETE_EDIT_CONFIG) || o.getValue()
                            .equals(REMOVE_EDIT_CONFIG))) {
                return true;
            }

        }

        for (final XmlElement xmlElement : element.getChildElements()) {
            if (containsDelete(xmlElement)) {
                return true;
            }

        }

        return false;
    }
}
