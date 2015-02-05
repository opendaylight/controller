/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.runtimerpc;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.management.ObjectName;
import javax.management.openmbean.OpenType;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry.Rpc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.VoidAttribute;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.mapping.AttributeMappingStrategy;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.mapping.ObjectMapper;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.toxml.ObjectXmlWriter;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.rpc.InstanceRuntimeRpc;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.rpc.ModuleRpcs;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.rpc.Rpcs;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.AbstractConfigNetconfOperation;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreContext;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.util.exception.MissingNameSpaceException;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RuntimeRpc extends AbstractConfigNetconfOperation {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeRpc.class);
    public static final String CONTEXT_INSTANCE = "context-instance";

    private final YangStoreContext yangStoreSnapshot;

    public RuntimeRpc(final YangStoreContext yangStoreSnapshot, ConfigRegistryClient configRegistryClient,
            String netconfSessionIdForReporting) {
        super(configRegistryClient, netconfSessionIdForReporting);
        this.yangStoreSnapshot = yangStoreSnapshot;
    }

    private Element toXml(Document doc, Object result, AttributeIfc returnType, String namespace, String elementName) throws NetconfDocumentedException {
        AttributeMappingStrategy<?, ? extends OpenType<?>> mappingStrategy = new ObjectMapper().prepareStrategy(returnType);
        Optional<?> mappedAttributeOpt = mappingStrategy.mapAttribute(result);
        Preconditions.checkState(mappedAttributeOpt.isPresent(), "Unable to map return value %s as %s", result, returnType.getOpenType());

        // FIXME: multiple return values defined as leaf-list and list in yang should not be wrapped in output xml element,
        // they need to be appended directly under rpc-reply element
        //
        // Either allow List of Elements to be returned from NetconfOperation or
        // pass reference to parent output xml element for netconf operations to
        // append result(s) on their own
        Element tempParent = XmlUtil.createElement(doc, "output", Optional.of(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0));
        new ObjectXmlWriter().prepareWritingStrategy(elementName, returnType, doc).writeElement(tempParent, namespace, mappedAttributeOpt.get());

        XmlElement xmlElement = XmlElement.fromDomElement(tempParent);
        return xmlElement.getChildElements().size() > 1 ? tempParent : xmlElement.getOnlyChildElement().getDomElement();
    }

    private Object executeOperation(final ConfigRegistryClient configRegistryClient, final ObjectName on,
            final String name, final Map<String, AttributeConfigElement> attributes) {
        final Object[] params = new Object[attributes.size()];
        final String[] signature = new String[attributes.size()];

        int i = 0;
        for (final AttributeConfigElement attribute : attributes.values()) {
            final Optional<?> resolvedValueOpt = attribute.getResolvedValue();

            params[i] = resolvedValueOpt.isPresent() ? resolvedValueOpt.get() : attribute.getResolvedDefaultValue();
            signature[i] = resolvedValueOpt.isPresent() ? resolvedValueOpt.get().getClass().getName() : attribute
                    .getResolvedDefaultValue().getClass().getName();
            i++;
        }

        return configRegistryClient.invokeMethod(on, name, params, signature);
    }

    public NetconfOperationExecution fromXml(final XmlElement xml) throws NetconfDocumentedException {
        final String namespace;
        try {
            namespace = xml.getNamespace();
        } catch (MissingNameSpaceException e) {
            LOG.trace("Can't get namespace from xml element due to ",e);
            throw NetconfDocumentedException.wrap(e);
        }
        final XmlElement contextInstanceElement = xml.getOnlyChildElement(CONTEXT_INSTANCE);
        final String operationName = xml.getName();

        final RuntimeRpcElementResolved id = RuntimeRpcElementResolved.fromXpath(
                contextInstanceElement.getTextContent(), operationName, namespace);

        final Rpcs rpcs = mapRpcs(yangStoreSnapshot.getModuleMXBeanEntryMap());

        final ModuleRpcs rpcMapping = rpcs.getRpcMapping(id);
        final InstanceRuntimeRpc instanceRuntimeRpc = rpcMapping.getRpc(id.getRuntimeBeanName(), operationName);

        // TODO move to Rpcs after xpath attribute is redesigned

        final ObjectName on = id.getObjectName(rpcMapping);
        Map<String, AttributeConfigElement> attributes = instanceRuntimeRpc.fromXml(xml);
        attributes = sortAttributes(attributes, xml);

        return new NetconfOperationExecution(on, instanceRuntimeRpc.getName(), attributes,
                instanceRuntimeRpc.getReturnType(), namespace);
    }

    @Override
    public HandlingPriority canHandle(Document message) throws NetconfDocumentedException {
        XmlElement requestElement = null;
        requestElement = getRequestElementWithCheck(message);

        XmlElement operationElement = requestElement.getOnlyChildElement();
        final String netconfOperationName = operationElement.getName();
        final String netconfOperationNamespace;
        try {
            netconfOperationNamespace = operationElement.getNamespace();
        } catch (MissingNameSpaceException e) {
            LOG.debug("Cannot retrieve netconf operation namespace from message due to ", e);
            return HandlingPriority.CANNOT_HANDLE;
        }

        final Optional<XmlElement> contextInstanceElement = operationElement
                .getOnlyChildElementOptionally(CONTEXT_INSTANCE);

        if (!contextInstanceElement.isPresent()){
            return HandlingPriority.CANNOT_HANDLE;
        }

        final RuntimeRpcElementResolved id = RuntimeRpcElementResolved.fromXpath(contextInstanceElement.get()
                .getTextContent(), netconfOperationName, netconfOperationNamespace);

        // TODO reuse rpcs instance in fromXml method
        final Rpcs rpcs = mapRpcs(yangStoreSnapshot.getModuleMXBeanEntryMap());

        try {

            final ModuleRpcs rpcMapping = rpcs.getRpcMapping(id);
            final InstanceRuntimeRpc instanceRuntimeRpc = rpcMapping.getRpc(id.getRuntimeBeanName(),
                    netconfOperationName);
            Preconditions.checkState(instanceRuntimeRpc != null, "No rpc found for %s:%s", netconfOperationNamespace,
                    netconfOperationName);

        } catch (IllegalStateException e) {
            LOG.debug("Cannot handle runtime operation {}:{}", netconfOperationNamespace, netconfOperationName, e);
            return HandlingPriority.CANNOT_HANDLE;
        }

        return HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY;
    }

    @Override
    protected HandlingPriority canHandle(String netconfOperationName, String namespace) {
        throw new UnsupportedOperationException(
                "This should not be used since it is not possible to provide check with these attributes");
    }

    @Override
    protected String getOperationName() {
        throw new UnsupportedOperationException("Runtime rpc does not have a stable name");
    }

    @Override
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement xml) throws NetconfDocumentedException {
        // TODO check for namespaces and unknown elements
        final NetconfOperationExecution execution = fromXml(xml);

        LOG.debug("Invoking operation {} on {} with arguments {}", execution.operationName, execution.on,
                execution.attributes);
        final Object result = executeOperation(getConfigRegistryClient(), execution.on, execution.operationName,
                execution.attributes);

        LOG.trace("Operation {} called successfully on {} with arguments {} with result {}", execution.operationName,
                execution.on, execution.attributes, result);

        if (execution.isVoid()) {
            return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
        } else {
            return toXml(document, result, execution.returnType, execution.namespace,
                    execution.returnType.getAttributeYangName());
        }
    }

    private static class NetconfOperationExecution {

        private final ObjectName on;
        private final String operationName;
        private final Map<String, AttributeConfigElement> attributes;
        private final AttributeIfc returnType;
        private final String namespace;

        public NetconfOperationExecution(final ObjectName on, final String name,
                final Map<String, AttributeConfigElement> attributes, final AttributeIfc returnType, final String namespace) {
            this.on = on;
            this.operationName = name;
            this.attributes = attributes;
            this.returnType = returnType;
            this.namespace = namespace;
        }

        boolean isVoid() {
            return returnType == VoidAttribute.getInstance();
        }

    }

    private static Map<String, AttributeConfigElement> sortAttributes(
            final Map<String, AttributeConfigElement> attributes, final XmlElement xml) {
        final Map<String, AttributeConfigElement> sorted = Maps.newLinkedHashMap();

        for (XmlElement xmlElement : xml.getChildElements()) {
            final String name = xmlElement.getName();
            if (!CONTEXT_INSTANCE.equals(name)) { // skip context
                                                          // instance child node
                                                          // because it
                                                          // specifies
                // ObjectName
                final AttributeConfigElement value = attributes.get(name);
                if (value == null) {
                    throw new IllegalArgumentException("Cannot find yang mapping for node " + xmlElement);
                }
                sorted.put(name, value);
            }
        }

        return sorted;
    }

    private static Rpcs mapRpcs(final Map<String, Map<String, ModuleMXBeanEntry>> mBeanEntries) {

        final Map<String, Map<String, ModuleRpcs>> map = Maps.newHashMap();

        for (final Map.Entry<String, Map<String, ModuleMXBeanEntry>> namespaceToModuleEntry : mBeanEntries.entrySet()) {

            Map<String, ModuleRpcs> namespaceToModules = map.get(namespaceToModuleEntry.getKey());
            if (namespaceToModules == null) {
                namespaceToModules = Maps.newHashMap();
                map.put(namespaceToModuleEntry.getKey(), namespaceToModules);
            }

            for (final Map.Entry<String, ModuleMXBeanEntry> moduleEntry : namespaceToModuleEntry.getValue().entrySet()) {

                ModuleRpcs rpcMapping = namespaceToModules.get(moduleEntry.getKey());
                if (rpcMapping == null) {
                    rpcMapping = new ModuleRpcs();
                    namespaceToModules.put(moduleEntry.getKey(), rpcMapping);
                }

                final ModuleMXBeanEntry entry = moduleEntry.getValue();

                for (final RuntimeBeanEntry runtimeEntry : entry.getRuntimeBeans()) {
                    rpcMapping.addNameMapping(runtimeEntry);
                    for (final Rpc rpc : runtimeEntry.getRpcs()) {
                        rpcMapping.addRpc(runtimeEntry, rpc);
                    }
                }
            }
        }

        return new Rpcs(map);
    }

}
