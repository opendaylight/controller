/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.getconfig;

import com.google.common.base.Optional;
import java.util.Set;
import javax.management.ObjectName;
import org.opendaylight.controller.config.persist.mapping.mapping.config.Config;
import org.opendaylight.controller.config.persist.mapping.mapping.config.ServiceRegistryWrapper;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.AbstractConfigNetconfOperation;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.Datastore;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfig;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreContext;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GetConfig extends AbstractConfigNetconfOperation {

    public static final String GET_CONFIG = "get-config";

    private final YangStoreContext yangStoreSnapshot;
    private final Optional<String> maybeNamespace;

    private final TransactionProvider transactionProvider;

    private static final Logger LOG = LoggerFactory.getLogger(GetConfig.class);

    public GetConfig(YangStoreContext yangStoreSnapshot, Optional<String> maybeNamespace,
            TransactionProvider transactionProvider, ConfigRegistryClient configRegistryClient,
            String netconfSessionIdForReporting) {
        super(configRegistryClient, netconfSessionIdForReporting);
        this.yangStoreSnapshot = yangStoreSnapshot;
        this.maybeNamespace = maybeNamespace;
        this.transactionProvider = transactionProvider;
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

    private Element getResponseInternal(final Document document, final ConfigRegistryClient configRegistryClient,
            final Datastore source) {

        final ConfigTransactionClient registryClient;
        // Read current state from a transaction, if running is source, then start new transaction just for reading
        // in case of candidate, get current transaction representing candidate
        if(source == Datastore.running) {
            final ObjectName readTx = transactionProvider.getOrCreateReadTransaction();
            registryClient = getConfigRegistryClient().getConfigTransactionClient(readTx);
        } else {
            registryClient  = getConfigRegistryClient().getConfigTransactionClient(transactionProvider.getOrCreateTransaction());
        }

        try {
            Element dataElement = XmlUtil.createElement(document, XmlNetconfConstants.DATA_KEY, Optional.<String>absent());
            final Set<ObjectName> instances = Datastore.getInstanceQueryStrategy(source, this.transactionProvider)
                    .queryInstances(configRegistryClient);

            final Config configMapping = new Config(EditConfig.transformMbeToModuleConfigs(registryClient,
                    yangStoreSnapshot.getModuleMXBeanEntryMap()), yangStoreSnapshot.getEnumResolver());

            ServiceRegistryWrapper serviceTracker = new ServiceRegistryWrapper(registryClient);
            dataElement = configMapping.toXml(instances, this.maybeNamespace, document, dataElement, serviceTracker);

            LOG.trace("{} operation successful", GET_CONFIG);

            return dataElement;
        } finally {
            if(source == Datastore.running) {
                transactionProvider.closeReadTransaction();
            }
        }
    }

    @Override
    protected String getOperationName() {
        return GET_CONFIG;
    }

    @Override
    public Element handleWithNoSubsequentOperations(Document document, XmlElement xml) throws DocumentedException {
        Datastore source;
        source = fromXml(xml);
        return getResponseInternal(document, getConfigRegistryClient(), source);
    }
}
