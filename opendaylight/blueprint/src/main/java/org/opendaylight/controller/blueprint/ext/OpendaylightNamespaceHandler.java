/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.base.Strings;
import java.io.StringReader;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.ext.ComponentFactoryMetadata;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableReferenceMetadata;
import org.apache.aries.blueprint.mutable.MutableServiceMetadata;
import org.apache.aries.blueprint.mutable.MutableServiceReferenceMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.opendaylight.controller.blueprint.BlueprintContainerRestartService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * The NamespaceHandler for Opendaylight blueprint extensions.
 *
 * @author Thomas Pantelis
 */
public class OpendaylightNamespaceHandler implements NamespaceHandler {
    public static final String NAMESPACE_1_0_0 = "http://opendaylight.org/xmlns/blueprint/v1.0.0";
    static final String ROUTED_RPC_REG_CONVERTER_NAME = "org.opendaylight.blueprint.RoutedRpcRegConverter";
    static final String RPC_REGISTRY_NAME = "org.opendaylight.blueprint.RpcRegistry";
    static final String NOTIFICATION_SERVICE_NAME = "org.opendaylight.blueprint.NotificationService";

    private static final Logger LOG = LoggerFactory.getLogger(OpendaylightNamespaceHandler.class);
    private static final String COMPONENT_PROCESSOR_NAME = ComponentProcessor.class.getName();
    private static final String RESTART_DEPENDENTS_ON_UPDATES = "restart-dependents-on-updates";
    private static final String USE_DEFAULT_FOR_REFERENCE_TYPES = "use-default-for-reference-types";
    private static final String CLUSTERED_APP_CONFIG = "clustered-app-config";
    private static final String TYPE_ATTR = "type";
    private static final String INTERFACE = "interface";
    private static final String REF_ATTR = "ref";
    private static final String ID_ATTR = "id";
    private static final String RPC_SERVICE = "rpc-service";

    @SuppressWarnings("rawtypes")
    @Override
    public Set<Class> getManagedClasses() {
        return Collections.emptySet();
    }

    @Override
    public URL getSchemaLocation(String namespace) {
        if(NAMESPACE_1_0_0.equals(namespace)) {
            URL url = getClass().getResource("/opendaylight-blueprint-ext-1.0.0.xsd");
            LOG.debug("getSchemaLocation for {} returning URL {}", namespace, url);
            return url;
        }

        return null;
    }

    @Override
    public Metadata parse(Element element, ParserContext context) {
        LOG.debug("In parse for {}", element);

        if (nodeNameEquals(element, RpcImplementationBean.RPC_IMPLEMENTATION)) {
            return parseRpcImplementation(element, context);
        } else if (nodeNameEquals(element, RoutedRpcMetadata.ROUTED_RPC_IMPLEMENTATION)) {
            return parseRoutedRpcImplementation(element, context);
        } else if (nodeNameEquals(element, RPC_SERVICE)) {
            return parseRpcService(element, context);
        } else if (nodeNameEquals(element, NotificationListenerBean.NOTIFICATION_LISTENER)) {
            return parseNotificationListener(element, context);
        } else if (nodeNameEquals(element, CLUSTERED_APP_CONFIG)) {
            return parseClusteredAppConfig(element, context);
        }

        throw new ComponentDefinitionException("Unsupported standalone element: " + element.getNodeName());
    }

    @Override
    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        if(node instanceof Attr) {
            if (nodeNameEquals(node, RESTART_DEPENDENTS_ON_UPDATES)) {
                return decorateRestartDependentsOnUpdates((Attr)node, component, context);
            } else if (nodeNameEquals(node, USE_DEFAULT_FOR_REFERENCE_TYPES)) {
                return decorateUseDefaultForReferenceTypes((Attr)node, component, context);
            } else if (nodeNameEquals(node, TYPE_ATTR)) {
                if(component instanceof ServiceReferenceMetadata) {
                    return decorateServiceReferenceType((Attr)node, component, context);
                } else if(component instanceof ServiceMetadata) {
                    return decorateServiceType((Attr)node, component, context);
                }

                throw new ComponentDefinitionException("Attribute " + node.getNodeName() +
                        " can only be used on a <reference>, <reference-list> or <service> element");
            }

            throw new ComponentDefinitionException("Unsupported attribute: " + node.getNodeName());
        } else {
            throw new ComponentDefinitionException("Unsupported node type: " + node);
        }
    }

    private ComponentMetadata decorateServiceType(Attr attr, ComponentMetadata component, ParserContext context) {
        if (!(component instanceof MutableServiceMetadata)) {
            throw new ComponentDefinitionException("Expected an instanceof MutableServiceMetadata");
        }

        MutableServiceMetadata service = (MutableServiceMetadata)component;

        LOG.debug("decorateServiceType for {} - adding type property {}", service.getId(), attr.getValue());

        service.addServiceProperty(createValue(context, TYPE_ATTR), createValue(context, attr.getValue()));
        return component;
    }

    private ComponentMetadata decorateServiceReferenceType(Attr attr, ComponentMetadata component, ParserContext context) {
        if (!(component instanceof MutableServiceReferenceMetadata)) {
            throw new ComponentDefinitionException("Expected an instanceof MutableServiceReferenceMetadata");
        }

        // We don't actually need the ComponentProcessor for augmenting the OSGi filter here but we create it
        // to workaround an issue in Aries where it doesn't use the extended filter unless there's a
        // Processor or ComponentDefinitionRegistryProcessor registered. This may actually be working as
        // designed in Aries b/c the extended filter was really added to allow the OSGi filter to be
        // substituted by a variable via the "cm:property-placeholder" processor. If so, it's a bit funky
        // but as long as there's at least one processor registered, it correctly uses the extended filter.
        registerComponentProcessor(context);

        MutableServiceReferenceMetadata serviceRef = (MutableServiceReferenceMetadata)component;
        String oldFilter = serviceRef.getExtendedFilter() == null ? null :
            serviceRef.getExtendedFilter().getStringValue();

        String filter;
        if(Strings.isNullOrEmpty(oldFilter)) {
            filter = String.format("(type=%s)", attr.getValue());
        } else {
            filter = String.format("(&(%s)(type=%s))", oldFilter, attr.getValue());
        }

        LOG.debug("decorateServiceReferenceType for {} with type {}, old filter: {}, new filter: {}",
                serviceRef.getId(), attr.getValue(), oldFilter, filter);

        serviceRef.setExtendedFilter(createValue(context, filter));
        return component;
    }

    private static ComponentMetadata decorateRestartDependentsOnUpdates(Attr attr, ComponentMetadata component,
            ParserContext context) {
        return enableComponentProcessorProperty(attr, component, context, "restartDependentsOnUpdates");
    }

    private static ComponentMetadata decorateUseDefaultForReferenceTypes(Attr attr, ComponentMetadata component,
            ParserContext context) {
        return enableComponentProcessorProperty(attr, component, context, "useDefaultForReferenceTypes");
    }

    private static ComponentMetadata enableComponentProcessorProperty(Attr attr, ComponentMetadata component,
            ParserContext context, String propertyName) {
        if(component != null) {
            throw new ComponentDefinitionException("Attribute " + attr.getNodeName() +
                    " can only be used on the root <blueprint> element");
        }

        LOG.debug("{}: {}", propertyName, attr.getValue());

        if(!Boolean.parseBoolean(attr.getValue())) {
            return component;
        }

        MutableBeanMetadata metadata = registerComponentProcessor(context);
        metadata.addProperty(propertyName, createValue(context, "true"));

        return component;
    }

    private static MutableBeanMetadata registerComponentProcessor(ParserContext context) {
        ComponentDefinitionRegistry registry = context.getComponentDefinitionRegistry();
        MutableBeanMetadata metadata = (MutableBeanMetadata) registry.getComponentDefinition(COMPONENT_PROCESSOR_NAME);
        if(metadata == null) {
            metadata = context.createMetadata(MutableBeanMetadata.class);
            metadata.setProcessor(true);
            metadata.setId(COMPONENT_PROCESSOR_NAME);
            metadata.setActivation(BeanMetadata.ACTIVATION_EAGER);
            metadata.setScope(BeanMetadata.SCOPE_SINGLETON);
            metadata.setRuntimeClass(ComponentProcessor.class);
            metadata.setDestroyMethod("destroy");
            metadata.addProperty("bundle", createRef(context, "blueprintBundle"));
            metadata.addProperty("blueprintContainerRestartService", createServiceRef(context,
                    BlueprintContainerRestartService.class, null));

            LOG.debug("Registering ComponentProcessor bean: {}", metadata);

            registry.registerComponentDefinition(metadata);
        }

        return metadata;
    }

    private Metadata parseRpcImplementation(Element element, ParserContext context) {
        registerRpcRegistryServiceRefBean(context);

        MutableBeanMetadata metadata = context.createMetadata(MutableBeanMetadata.class);
        metadata.setId(context.generateId());
        metadata.setScope(BeanMetadata.SCOPE_SINGLETON);
        metadata.setActivation(ReferenceMetadata.ACTIVATION_EAGER);
        metadata.setRuntimeClass(RpcImplementationBean.class);
        metadata.setInitMethod("init");
        metadata.setDestroyMethod("destroy");
        metadata.addProperty("bundle", createRef(context, "blueprintBundle"));
        metadata.addProperty("rpcRegistry", createRef(context, RPC_REGISTRY_NAME));
        metadata.addProperty("implementation", createRef(context, element.getAttribute(REF_ATTR)));

        if(element.hasAttribute(INTERFACE)) {
            metadata.addProperty("interfaceName", createValue(context, element.getAttribute(INTERFACE)));
        }

        LOG.debug("parseAddRpcImplementation returning {}", metadata);

        return metadata;
    }

    private Metadata parseRoutedRpcImplementation(Element element, ParserContext context) {
        registerRpcRegistryServiceRefBean(context);
        registerRoutedRpcRegistrationConverter(context);

        ComponentFactoryMetadata metadata = new RoutedRpcMetadata(getId(context, element),
                element.getAttribute(INTERFACE), element.getAttribute(REF_ATTR));

        LOG.debug("parseRoutedRpcImplementation returning {}", metadata);

        return metadata;
    }

    private Metadata parseRpcService(Element element, ParserContext context) {
        ComponentFactoryMetadata metadata = new RpcServiceMetadata(getId(context, element),
                element.getAttribute(INTERFACE));

        LOG.debug("parseRpcService returning {}", metadata);

        return metadata;
    }

    private void registerRoutedRpcRegistrationConverter(ParserContext context) {
        ComponentDefinitionRegistry registry = context.getComponentDefinitionRegistry();
        if(registry.getComponentDefinition(ROUTED_RPC_REG_CONVERTER_NAME) == null) {
            MutableBeanMetadata metadata = context.createMetadata(MutableBeanMetadata.class);
            metadata.setId(ROUTED_RPC_REG_CONVERTER_NAME);
            metadata.setScope(BeanMetadata.SCOPE_SINGLETON);
            metadata.setActivation(ReferenceMetadata.ACTIVATION_LAZY);
            metadata.setRuntimeClass(RoutedRpcRegistrationConverter.class);
            registry.registerTypeConverter(metadata);
        }
    }

    private void registerRpcRegistryServiceRefBean(ParserContext context) {
        ComponentDefinitionRegistry registry = context.getComponentDefinitionRegistry();
        if(registry.getComponentDefinition(RPC_REGISTRY_NAME) == null) {
            MutableReferenceMetadata metadata = createServiceRef(context, RpcProviderRegistry.class, null);
            metadata.setId(RPC_REGISTRY_NAME);
            registry.registerComponentDefinition(metadata);
        }
    }

    private Metadata parseNotificationListener(Element element, ParserContext context) {
        registerNotificationServiceRefBean(context);

        MutableBeanMetadata metadata = context.createMetadata(MutableBeanMetadata.class);
        metadata.setId(context.generateId());
        metadata.setScope(BeanMetadata.SCOPE_SINGLETON);
        metadata.setActivation(ReferenceMetadata.ACTIVATION_EAGER);
        metadata.setRuntimeClass(NotificationListenerBean.class);
        metadata.setInitMethod("init");
        metadata.setDestroyMethod("destroy");
        metadata.addProperty("bundle", createRef(context, "blueprintBundle"));
        metadata.addProperty("notificationService", createRef(context, NOTIFICATION_SERVICE_NAME));
        metadata.addProperty("notificationListener", createRef(context, element.getAttribute(REF_ATTR)));

        LOG.debug("parseNotificationListener returning {}", metadata);

        return metadata;
    }

    private void registerNotificationServiceRefBean(ParserContext context) {
        ComponentDefinitionRegistry registry = context.getComponentDefinitionRegistry();
        if(registry.getComponentDefinition(NOTIFICATION_SERVICE_NAME) == null) {
            MutableReferenceMetadata metadata = createServiceRef(context, NotificationService.class, null);
            metadata.setId(NOTIFICATION_SERVICE_NAME);
            registry.registerComponentDefinition(metadata);
        }
    }

    private Metadata parseClusteredAppConfig(Element element, ParserContext context) {
        LOG.debug("parseClusteredAppConfig");

        // Find the default-config child element representing the default app config XML, if present.
        Element defaultConfigElement = null;
        NodeList children = element.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if(nodeNameEquals(child, DataStoreAppConfigMetadata.DEFAULT_CONFIG)) {
                defaultConfigElement = (Element) child;
                break;
            }
        }

        Element defaultAppConfigElement = null;
        if(defaultConfigElement != null) {
            // Find the CDATA element containing the default app config XML.
            children = defaultConfigElement.getChildNodes();
            for(int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if(child.getNodeType() == Node.CDATA_SECTION_NODE) {
                    defaultAppConfigElement = parseXML(DataStoreAppConfigMetadata.DEFAULT_CONFIG, child.getTextContent());
                    break;
                }
            }
        }

        return new DataStoreAppConfigMetadata(getId(context, element), element.getAttribute(
                DataStoreAppConfigMetadata.BINDING_CLASS), element.getAttribute(
                        DataStoreAppConfigMetadata.LIST_KEY_VALUE), defaultAppConfigElement);
    }

    private Element parseXML(String name, String xml) {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        builderFactory.setCoalescing(true);
        builderFactory.setIgnoringElementContentWhitespace(true);
        builderFactory.setIgnoringComments(true);

        try {
            return builderFactory.newDocumentBuilder().parse(new InputSource(new StringReader(xml))).getDocumentElement();
        } catch(Exception e) {
            throw new ComponentDefinitionException(String.format("Error %s parsing XML: %s", name, xml));
        }
    }

    private static ValueMetadata createValue(ParserContext context, String value) {
        MutableValueMetadata m = context.createMetadata(MutableValueMetadata.class);
        m.setStringValue(value);
        return m;
    }

    private static MutableReferenceMetadata createServiceRef(ParserContext context, Class<?> cls, String filter) {
        MutableReferenceMetadata m = context.createMetadata(MutableReferenceMetadata.class);
        m.setRuntimeInterface(cls);
        m.setInterface(cls.getName());
        m.setActivation(ReferenceMetadata.ACTIVATION_EAGER);
        m.setAvailability(ReferenceMetadata.AVAILABILITY_MANDATORY);

        if(filter != null) {
            m.setFilter(filter);
        }

        return m;
    }

    private static RefMetadata createRef(ParserContext context, String id) {
        MutableRefMetadata metadata = context.createMetadata(MutableRefMetadata.class);
        metadata.setComponentId(id);
        return metadata;
    }

    private static String getId(ParserContext context, Element element) {
        if(element.hasAttribute(ID_ATTR)) {
            return element.getAttribute(ID_ATTR);
        } else {
            return context.generateId();
        }
    }

    private static boolean nodeNameEquals(Node node, String name) {
        return name.equals(node.getNodeName()) || name.equals(node.getLocalName());
    }
}
