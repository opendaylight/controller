/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.get;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreSnapshot;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.InstanceConfig;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ModuleConfig;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ServiceRegistryWrapper;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.runtime.InstanceRuntime;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.runtime.ModuleRuntime;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.runtime.Runtime;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.AbstractConfigNetconfOperation;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.Datastore;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfig;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.Maps;

public class Get extends AbstractConfigNetconfOperation {

    private final YangStoreSnapshot yangStoreSnapshot;
    private static final Logger logger = LoggerFactory.getLogger(Get.class);
    private final TransactionProvider transactionProvider;

    public Get(YangStoreSnapshot yangStoreSnapshot, ConfigRegistryClient configRegistryClient,
               String netconfSessionIdForReporting, TransactionProvider transactionProvider) {
        super(configRegistryClient, netconfSessionIdForReporting);
        this.yangStoreSnapshot = yangStoreSnapshot;
        this.transactionProvider = transactionProvider;
    }

    private Map<String, Map<String, ModuleRuntime>> createModuleRuntimes(ConfigRegistryClient configRegistryClient,
            Map<String, Map<String, ModuleMXBeanEntry>> mBeanEntries) {
        Map<String, Map<String, ModuleRuntime>> retVal = Maps.newHashMap();

        for (String namespace : mBeanEntries.keySet()) {

            Map<String, ModuleRuntime> innerMap = Maps.newHashMap();
            Map<String, ModuleMXBeanEntry> entriesFromNamespace = mBeanEntries.get(namespace);
            for (String module : entriesFromNamespace.keySet()) {

                ModuleMXBeanEntry mbe = entriesFromNamespace.get(module);

                Map<RuntimeBeanEntry, InstanceConfig> cache = Maps.newHashMap();
                RuntimeBeanEntry root = null;
                for (RuntimeBeanEntry rbe : mbe.getRuntimeBeans()) {
                    cache.put(rbe, new InstanceConfig(configRegistryClient, rbe.getYangPropertiesToTypesMap()));
                    if (rbe.isRoot())
                        root = rbe;
                }

                if (root == null)
                    continue;

                InstanceRuntime rootInstanceRuntime = createInstanceRuntime(root, cache);
                ModuleRuntime moduleRuntime = new ModuleRuntime(module, rootInstanceRuntime);
                innerMap.put(module, moduleRuntime);
            }

            retVal.put(namespace, innerMap);
        }
        return retVal;
    }

    private InstanceRuntime createInstanceRuntime(RuntimeBeanEntry root, Map<RuntimeBeanEntry, InstanceConfig> cache) {
        Map<String, InstanceRuntime> children = Maps.newHashMap();
        for (RuntimeBeanEntry child : root.getChildren()) {
            children.put(child.getJavaNamePrefix(), createInstanceRuntime(child, cache));
        }

        return new InstanceRuntime(cache.get(root), children, createJmxToYangMap(root.getChildren()));
    }

    private Map<String, String> createJmxToYangMap(List<RuntimeBeanEntry> children) {
        Map<String, String> jmxToYangNamesForChildRbe = Maps.newHashMap();
        for (RuntimeBeanEntry rbe : children) {
            jmxToYangNamesForChildRbe.put(rbe.getJavaNamePrefix(), rbe.getYangName());
        }
        return jmxToYangNamesForChildRbe;
    }

    private static void checkXml(XmlElement xml) {
        xml.checkName(XmlNetconfConstants.GET);
        xml.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

        // Filter option - unsupported
        if (xml.getChildElements(XmlNetconfConstants.FILTER).size() != 0)
            throw new UnsupportedOperationException("Unsupported option " + XmlNetconfConstants.FILTER + " for " + XmlNetconfConstants.GET);
    }

    @Override
    protected String getOperationName() {
        return XmlNetconfConstants.GET;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement xml) throws NetconfDocumentedException {
        try {
            checkXml(xml);
        } catch (final IllegalArgumentException e) {
            logger.warn("Error parsing xml", e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(ErrorTag.bad_attribute.name(), e.getMessage());
            throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.rpc, ErrorTag.bad_attribute,
                    ErrorSeverity.error, errorInfo);
        } catch (final UnsupportedOperationException e) {
            logger.warn("Unsupported", e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(ErrorTag.operation_not_supported.name(), "Unsupported option for 'get'");
            throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.application,
                    ErrorTag.operation_not_supported, ErrorSeverity.error, errorInfo);
        }

        final Set<ObjectName> runtimeBeans = configRegistryClient.lookupRuntimeBeans();

        //Transaction provider required only for candidate datastore
        final Set<ObjectName> configBeans = Datastore.getInstanceQueryStrategy(Datastore.running, null)
                .queryInstances(configRegistryClient);

        final Map<String, Map<String, ModuleRuntime>> moduleRuntimes = createModuleRuntimes(configRegistryClient,
                yangStoreSnapshot.getModuleMXBeanEntryMap());
        final Map<String, Map<String, ModuleConfig>> moduleConfigs = EditConfig.transformMbeToModuleConfigs(
                configRegistryClient, yangStoreSnapshot.getModuleMXBeanEntryMap());

        final Runtime runtime = new Runtime(moduleRuntimes, moduleConfigs);

        ObjectName txOn = transactionProvider.getOrCreateTransaction();
        ConfigTransactionClient ta = configRegistryClient.getConfigTransactionClient(txOn);
        final Element element = runtime.toXml(runtimeBeans, configBeans, document, new ServiceRegistryWrapper(ta));

        logger.trace("{} operation successful", XmlNetconfConstants.GET);

        return element;
    }
}
