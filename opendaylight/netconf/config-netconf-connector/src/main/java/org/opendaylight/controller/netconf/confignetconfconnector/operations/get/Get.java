/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.get;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.ObjectName;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.InstanceConfig;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ModuleConfig;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.runtime.InstanceRuntime;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.runtime.ModuleRuntime;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.runtime.Runtime;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.AbstractConfigNetconfOperation;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.Datastore;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfig;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreContext;
import org.opendaylight.controller.netconf.util.exception.MissingNameSpaceException;
import org.opendaylight.controller.netconf.util.exception.UnexpectedElementException;
import org.opendaylight.controller.netconf.util.exception.UnexpectedNamespaceException;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Get extends AbstractConfigNetconfOperation {

    private final YangStoreContext yangStoreSnapshot;
    private static final Logger LOG = LoggerFactory.getLogger(Get.class);

    public Get(YangStoreContext yangStoreSnapshot, ConfigRegistryClient configRegistryClient,
               String netconfSessionIdForReporting) {
        super(configRegistryClient, netconfSessionIdForReporting);
        this.yangStoreSnapshot = yangStoreSnapshot;
    }

    private Map<String, Map<String, ModuleRuntime>> createModuleRuntimes(ConfigRegistryClient configRegistryClient,
            Map<String, Map<String, ModuleMXBeanEntry>> mBeanEntries) {
        Map<String, Map<String, ModuleRuntime>> retVal = Maps.newHashMap();

        for (Map.Entry<String, Map<String, ModuleMXBeanEntry>> namespaceToModuleEntry : mBeanEntries.entrySet()) {

            Map<String, ModuleRuntime> innerMap = Maps.newHashMap();
            Map<String, ModuleMXBeanEntry> entriesFromNamespace = namespaceToModuleEntry.getValue();
            for (Map.Entry<String, ModuleMXBeanEntry> moduleToMXEntry : entriesFromNamespace.entrySet()) {

                ModuleMXBeanEntry mbe = moduleToMXEntry.getValue();

                Map<RuntimeBeanEntry, InstanceConfig> cache = Maps.newHashMap();
                RuntimeBeanEntry root = null;
                for (RuntimeBeanEntry rbe : mbe.getRuntimeBeans()) {
                    cache.put(rbe, new InstanceConfig(configRegistryClient, rbe.getYangPropertiesToTypesMap(), mbe.getNullableDummyContainerName()));
                    if (rbe.isRoot()){
                        root = rbe;
                    }
                }

                if (root == null){
                    continue;
                }

                InstanceRuntime rootInstanceRuntime = createInstanceRuntime(root, cache);
                ModuleRuntime moduleRuntime = new ModuleRuntime(rootInstanceRuntime);
                innerMap.put(moduleToMXEntry.getKey(), moduleRuntime);
            }

            retVal.put(namespaceToModuleEntry.getKey(), innerMap);
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

    private static void checkXml(XmlElement xml) throws UnexpectedElementException, UnexpectedNamespaceException, MissingNameSpaceException {
        xml.checkName(XmlNetconfConstants.GET);
        xml.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

        // Filter option: ignore for now, TODO only load modules specified by the filter
    }

    @Override
    protected String getOperationName() {
        return XmlNetconfConstants.GET;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement xml) throws NetconfDocumentedException {
        checkXml(xml);

        final Set<ObjectName> runtimeBeans = getConfigRegistryClient().lookupRuntimeBeans();

        //Transaction provider required only for candidate datastore
        final Set<ObjectName> configBeans = Datastore.getInstanceQueryStrategy(Datastore.running, null)
                .queryInstances(getConfigRegistryClient());

        final Map<String, Map<String, ModuleRuntime>> moduleRuntimes = createModuleRuntimes(getConfigRegistryClient(),
                yangStoreSnapshot.getModuleMXBeanEntryMap());
        final Map<String, Map<String, ModuleConfig>> moduleConfigs = EditConfig.transformMbeToModuleConfigs(
                getConfigRegistryClient(), yangStoreSnapshot.getModuleMXBeanEntryMap());

        final Runtime runtime = new Runtime(moduleRuntimes, moduleConfigs);

        final Element element = runtime.toXml(runtimeBeans, configBeans, document);

        LOG.trace("{} operation successful", XmlNetconfConstants.GET);

        return element;
    }
}
