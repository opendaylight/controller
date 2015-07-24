/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.get;

import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacade;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.AbstractConfigNetconfOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Get extends AbstractConfigNetconfOperation {

    private static final Logger LOG = LoggerFactory.getLogger(Get.class);

    public Get(final ConfigSubsystemFacade configSubsystemFacade, final String netconfSessionIdForReporting) {
        super(configSubsystemFacade, netconfSessionIdForReporting);
    }

    private static void checkXml(XmlElement xml) throws DocumentedException {
        xml.checkName(XmlNetconfConstants.GET);
        xml.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

        // Filter option: ignore for now, TODO only load modules specified by the filter
    }

    @Override
    protected String getOperationName() {
        return XmlNetconfConstants.GET;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement xml) throws DocumentedException {
        checkXml(xml);
        final Element element = getConfigSubsystemFacade().get(document);
        LOG.trace("{} operation successful", XmlNetconfConstants.GET);
        return element;
    }
}
