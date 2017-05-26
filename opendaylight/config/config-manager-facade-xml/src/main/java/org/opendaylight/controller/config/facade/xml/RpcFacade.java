/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.management.ObjectName;
import javax.management.openmbean.OpenType;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.mapping.AttributeMappingStrategy;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.mapping.ObjectMapper;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.toxml.ObjectXmlWriter;
import org.opendaylight.controller.config.facade.xml.osgi.YangStoreService;
import org.opendaylight.controller.config.facade.xml.rpc.InstanceRuntimeRpc;
import org.opendaylight.controller.config.facade.xml.rpc.ModuleRpcs;
import org.opendaylight.controller.config.facade.xml.rpc.Rpcs;
import org.opendaylight.controller.config.facade.xml.rpc.RuntimeRpcElementResolved;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.util.xml.XmlMappingConstants;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.VoidAttribute;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RpcFacade {

    public static final String CONTEXT_INSTANCE = "context-instance";
    private YangStoreService yangStoreService;
    private ConfigRegistryClient configRegistryClient;

    public RpcFacade(final YangStoreService yangStoreService, final ConfigRegistryClient configRegistryClient) {
        this.yangStoreService = yangStoreService;
        this.configRegistryClient = configRegistryClient;
    }

    public Rpcs mapRpcs() {

        final Map<String, Map<String, ModuleRpcs>> map = Maps.newHashMap();

        for (final Map.Entry<String, Map<String, ModuleMXBeanEntry>> namespaceToModuleEntry : yangStoreService.getModuleMXBeanEntryMap().entrySet()) {

            Map<String, ModuleRpcs> namespaceToModules = map.get(namespaceToModuleEntry.getKey());
            if (namespaceToModules == null) {
                namespaceToModules = Maps.newHashMap();
                map.put(namespaceToModuleEntry.getKey(), namespaceToModules);
            }

            for (final Map.Entry<String, ModuleMXBeanEntry> moduleEntry : namespaceToModuleEntry.getValue().entrySet()) {

                ModuleRpcs rpcMapping = namespaceToModules.get(moduleEntry.getKey());
                if (rpcMapping == null) {
                    rpcMapping = new ModuleRpcs(yangStoreService.getEnumResolver());
                    namespaceToModules.put(moduleEntry.getKey(), rpcMapping);
                }

                final ModuleMXBeanEntry entry = moduleEntry.getValue();

                for (final RuntimeBeanEntry runtimeEntry : entry.getRuntimeBeans()) {
                    rpcMapping.addNameMapping(runtimeEntry);
                    for (final RuntimeBeanEntry.Rpc rpc : runtimeEntry.getRpcs()) {
                        rpcMapping.addRpc(runtimeEntry, rpc);
                    }
                }
            }
        }

        return new Rpcs(map);
    }


    public OperationExecution fromXml(final XmlElement xml) throws DocumentedException {
        final String namespace;
        namespace = xml.getNamespace();

        final XmlElement contextInstanceElement = xml.getOnlyChildElement(CONTEXT_INSTANCE);
        final String operationName = xml.getName();

        final RuntimeRpcElementResolved id = RuntimeRpcElementResolved.fromXpath(
                contextInstanceElement.getTextContent(), operationName, namespace);

        final Rpcs rpcs = mapRpcs();

        final ModuleRpcs rpcMapping = rpcs.getRpcMapping(id);
        final InstanceRuntimeRpc instanceRuntimeRpc = rpcMapping.getRpc(id.getRuntimeBeanName(), operationName);

        // TODO move to Rpcs after xpath attribute is redesigned

        final ObjectName on = id.getObjectName(rpcMapping);
        Map<String, AttributeConfigElement> attributes = instanceRuntimeRpc.fromXml(xml);
        attributes = sortAttributes(attributes, xml);

        return new OperationExecution(on, instanceRuntimeRpc.getName(), attributes,
                instanceRuntimeRpc.getReturnType(), namespace);
    }

    private Map<String, AttributeConfigElement> sortAttributes(
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

    public Object executeOperation(final OperationExecution execution) {
        final Object[] params = new Object[execution.attributes.size()];
        final String[] signature = new String[execution.attributes.size()];

        int i = 0;
        for (final AttributeConfigElement attribute : execution.attributes.values()) {
            final Optional<?> resolvedValueOpt = attribute.getResolvedValue();

            params[i] = resolvedValueOpt.isPresent() ? resolvedValueOpt.get() : attribute.getResolvedDefaultValue();
            signature[i] = resolvedValueOpt.isPresent() ? resolvedValueOpt.get().getClass().getName() : attribute
                    .getResolvedDefaultValue().getClass().getName();
            i++;
        }

        return configRegistryClient.invokeMethod(execution.on, execution.operationName, params, signature);
    }

    public Element toXml(Document doc, Object result, OperationExecution execution) throws DocumentedException {
        AttributeMappingStrategy<?, ? extends OpenType<?>> mappingStrategy = new ObjectMapper().prepareStrategy(execution.getReturnType());
        Optional<?> mappedAttributeOpt = mappingStrategy.mapAttribute(result);
        Preconditions.checkState(mappedAttributeOpt.isPresent(), "Unable to map return value %s as %s", result, execution.getReturnType().getOpenType());

        // FIXME: multiple return values defined as leaf-list and list in yang should not be wrapped in output xml element,
        // they need to be appended directly under rpc-reply element
        //
        // Either allow List of Elements to be returned from NetconfOperation or
        // pass reference to parent output xml element for netconf operations to
        // append result(s) on their own
        Element tempParent = XmlUtil.createElement(doc, "output", Optional.of(XmlMappingConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0));
        new ObjectXmlWriter().prepareWritingStrategy(execution.getReturnType().getAttributeYangName(),
                execution.getReturnType(), doc).writeElement(tempParent, execution.getNamespace(), mappedAttributeOpt.get());

        XmlElement xmlElement = XmlElement.fromDomElement(tempParent);
        return xmlElement.getChildElements().size() > 1 ? tempParent : xmlElement.getOnlyChildElement().getDomElement();
    }

    public class OperationExecution {

        private final ObjectName on;
        private final String operationName;
        private final Map<String, AttributeConfigElement> attributes;
        private final AttributeIfc returnType;
        private final String namespace;

        public OperationExecution(final ObjectName on, final String name,
            final Map<String, AttributeConfigElement> attributes, final AttributeIfc returnType, final String namespace) {
            this.on = on;
            this.operationName = name;
            this.attributes = attributes;
            this.returnType = returnType;
            this.namespace = namespace;
        }

        public boolean isVoid() {
            return returnType == VoidAttribute.getInstance();
        }

        public ObjectName getOn() {
            return on;
        }

        public String getOperationName() {
            return operationName;
        }

        public Map<String, AttributeConfigElement> getAttributes() {
            return attributes;
        }

        public AttributeIfc getReturnType() {
            return returnType;
        }

        public String getNamespace() {
            return namespace;
        }
    }

}
