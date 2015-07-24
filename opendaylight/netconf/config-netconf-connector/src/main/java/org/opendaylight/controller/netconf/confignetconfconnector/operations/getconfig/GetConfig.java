/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.getconfig;

import com.google.common.base.Optional;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacade;
import org.opendaylight.controller.config.facade.xml.Datastore;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.AbstractConfigNetconfOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GetConfig extends AbstractConfigNetconfOperation {

    public static final String GET_CONFIG = "get-config";

    private final Optional<String> maybeNamespace;

    private static final Logger LOG = LoggerFactory.getLogger(GetConfig.class);

    public GetConfig(final ConfigSubsystemFacade configSubsystemFacade, final Optional<String> maybeNamespace, final String netconfSessionIdForReporting) {
        super(configSubsystemFacade, netconfSessionIdForReporting);
        this.maybeNamespace = maybeNamespace;
    }

    public static Datastore fromXml(XmlElement xml) throws DocumentedException {

        xml.checkName(GET_CONFIG);
        xml.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

        XmlElement sourceElement = xml.getOnlyChildElement(XmlNetconfConstants.SOURCE_KEY,
                XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
        XmlElement sourceNode = sourceElement.getOnlyChildElement();
        String sourceParsed = sourceNode.getName();
        LOG.debug("Setting source datastore to '{}'", sourceParsed);
        Datastore sourceDatastore = Datastore.valueOf(sourceParsed);

        // Filter option: ignore for now, TODO only load modules specified by the filter

        return sourceDatastore;

    }

    @Override
    protected String getOperationName() {
        return GET_CONFIG;
    }

    @Override
    public Element handleWithNoSubsequentOperations(Document document, XmlElement xml) throws DocumentedException {
        return getConfigSubsystemFacade().getConfiguration(document, fromXml(xml), maybeNamespace);
    }
}
