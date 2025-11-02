/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.base.Strings;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Set;
import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableReferenceMetadata;
import org.apache.aries.blueprint.mutable.MutableServiceMetadata;
import org.apache.aries.blueprint.mutable.MutableServiceReferenceMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.opendaylight.controller.blueprint.BlueprintContainerRestartService;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
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
import org.xml.sax.SAXException;

/**
 * The NamespaceHandler for Opendaylight blueprint extensions.
 *
 * @author Thomas Pantelis
 */
public final class OpendaylightNamespaceHandler implements NamespaceHandler {
    public static final String NAMESPACE_1_0_0 = "http://opendaylight.org/xmlns/blueprint/v1.0.0";

    private static final Logger LOG = LoggerFactory.getLogger(OpendaylightNamespaceHandler.class);
    private static final String TYPE_ATTR = "type";
    private static final String UPDATE_STRATEGY_ATTR = "update-strategy";
    private static final String COMPONENT_PROCESSOR_NAME = ComponentProcessor.class.getName();
    private static final String RESTART_DEPENDENTS_ON_UPDATES = "restart-dependents-on-updates";
    private static final String USE_DEFAULT_FOR_REFERENCE_TYPES = "use-default-for-reference-types";
    private static final String CLUSTERED_APP_CONFIG = "clustered-app-config";
    private static final String ID_ATTR = "id";

    @SuppressWarnings("rawtypes")
    @Override
    public Set<Class> getManagedClasses() {
        return Set.of();
    }

    @Override
    public URL getSchemaLocation(final String namespace) {
        if (NAMESPACE_1_0_0.equals(namespace)) {
            URL url = getClass().getResource("/opendaylight-blueprint-ext-1.0.0.xsd");
            LOG.debug("getSchemaLocation for {} returning URL {}", namespace, url);
            return url;
        }

        return null;
    }

    @Override
    public Metadata parse(final Element element, final ParserContext context) {
        LOG.debug("In parse for {}", element);

        if (nodeNameEquals(element, CLUSTERED_APP_CONFIG)) {
            return parseClusteredAppConfig(element, context);
        }

        throw new ComponentDefinitionException("Unsupported standalone element: " + element.getNodeName());
    }

    @Override
    public ComponentMetadata decorate(final Node node, final ComponentMetadata component, final ParserContext context) {
        if (node instanceof Attr attr) {
            if (nodeNameEquals(node, RESTART_DEPENDENTS_ON_UPDATES)) {
                return decorateRestartDependentsOnUpdates(attr, component, context);
            } else if (nodeNameEquals(node, USE_DEFAULT_FOR_REFERENCE_TYPES)) {
                return decorateUseDefaultForReferenceTypes(attr, component, context);
            } else if (nodeNameEquals(node, TYPE_ATTR)) {
                if (component instanceof ServiceReferenceMetadata) {
                    return decorateServiceReferenceType(attr, component, context);
                } else if (component instanceof ServiceMetadata) {
                    return decorateServiceType(attr, component, context);
                }

                throw new ComponentDefinitionException("Attribute " + node.getNodeName()
                        + " can only be used on a <reference>, <reference-list> or <service> element");
            }

            throw new ComponentDefinitionException("Unsupported attribute: " + node.getNodeName());
        } else {
            throw new ComponentDefinitionException("Unsupported node type: " + node);
        }
    }

    private static ComponentMetadata decorateServiceType(final Attr attr, final ComponentMetadata component,
            final ParserContext context) {
        if (!(component instanceof MutableServiceMetadata service)) {
            throw new ComponentDefinitionException("Expected an instanceof MutableServiceMetadata");
        }

        LOG.debug("decorateServiceType for {} - adding type property {}", service.getId(), attr.getValue());

        service.addServiceProperty(createValue(context, TYPE_ATTR), createValue(context, attr.getValue()));
        return component;
    }

    private static ComponentMetadata decorateServiceReferenceType(final Attr attr, final ComponentMetadata component,
            final ParserContext context) {
        if (!(component instanceof MutableServiceReferenceMetadata serviceRef)) {
            throw new ComponentDefinitionException("Expected an instanceof MutableServiceReferenceMetadata");
        }

        // We don't actually need the ComponentProcessor for augmenting the OSGi filter here but we create it
        // to workaround an issue in Aries where it doesn't use the extended filter unless there's a
        // Processor or ComponentDefinitionRegistryProcessor registered. This may actually be working as
        // designed in Aries b/c the extended filter was really added to allow the OSGi filter to be
        // substituted by a variable via the "cm:property-placeholder" processor. If so, it's a bit funky
        // but as long as there's at least one processor registered, it correctly uses the extended filter.
        registerComponentProcessor(context);

        String oldFilter = serviceRef.getExtendedFilter() == null ? null :
            serviceRef.getExtendedFilter().getStringValue();

        String filter;
        if (Strings.isNullOrEmpty(oldFilter)) {
            filter = String.format("(type=%s)", attr.getValue());
        } else {
            filter = String.format("(&(%s)(type=%s))", oldFilter, attr.getValue());
        }

        LOG.debug("decorateServiceReferenceType for {} with type {}, old filter: {}, new filter: {}",
                serviceRef.getId(), attr.getValue(), oldFilter, filter);

        serviceRef.setExtendedFilter(createValue(context, filter));
        return component;
    }

    private static ComponentMetadata decorateRestartDependentsOnUpdates(final Attr attr,
            final ComponentMetadata component, final ParserContext context) {
        return enableComponentProcessorProperty(attr, component, context, "restartDependentsOnUpdates");
    }

    private static ComponentMetadata decorateUseDefaultForReferenceTypes(final Attr attr,
            final ComponentMetadata component, final ParserContext context) {
        return enableComponentProcessorProperty(attr, component, context, "useDefaultForReferenceTypes");
    }

    private static ComponentMetadata enableComponentProcessorProperty(final Attr attr,
            final ComponentMetadata component, final ParserContext context, final String propertyName) {
        if (component != null) {
            throw new ComponentDefinitionException("Attribute " + attr.getNodeName()
                    + " can only be used on the root <blueprint> element");
        }

        LOG.debug("Property {} = {}", propertyName, attr.getValue());

        if (!Boolean.parseBoolean(attr.getValue())) {
            return component;
        }

        MutableBeanMetadata metadata = registerComponentProcessor(context);
        metadata.addProperty(propertyName, createValue(context, "true"));

        return component;
    }

    private static MutableBeanMetadata registerComponentProcessor(final ParserContext context) {
        ComponentDefinitionRegistry registry = context.getComponentDefinitionRegistry();
        MutableBeanMetadata metadata = (MutableBeanMetadata) registry.getComponentDefinition(COMPONENT_PROCESSOR_NAME);
        if (metadata == null) {
            metadata = createBeanMetadata(context, COMPONENT_PROCESSOR_NAME, ComponentProcessor.class, false, true);
            metadata.setProcessor(true);
            addBlueprintBundleRefProperty(context, metadata);
            metadata.addProperty("blueprintContainerRestartService", createServiceRef(context,
                    BlueprintContainerRestartService.class, null));

            LOG.debug("Registering ComponentProcessor bean: {}", metadata);

            registry.registerComponentDefinition(metadata);
        }

        return metadata;
    }

    private static Metadata parseClusteredAppConfig(final Element element, final ParserContext context) {
        LOG.debug("parseClusteredAppConfig");

        // Find the default-config child element representing the default app config XML, if present.
        Element defaultConfigElement = null;
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (nodeNameEquals(child, DataStoreAppConfigMetadata.DEFAULT_CONFIG)) {
                defaultConfigElement = (Element) child;
                break;
            }
        }

        Element defaultAppConfigElement = null;
        if (defaultConfigElement != null) {
            // Find the CDATA element containing the default app config XML.
            children = defaultConfigElement.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
                    defaultAppConfigElement = parseXML(DataStoreAppConfigMetadata.DEFAULT_CONFIG,
                            child.getTextContent());
                    break;
                }
            }
        }

        return new DataStoreAppConfigMetadata(getId(context, element), element.getAttribute(
                DataStoreAppConfigMetadata.BINDING_CLASS), element.getAttribute(
                        DataStoreAppConfigMetadata.LIST_KEY_VALUE), element.getAttribute(
                        DataStoreAppConfigMetadata.DEFAULT_CONFIG_FILE_NAME), parseUpdateStrategy(
                        element.getAttribute(UPDATE_STRATEGY_ATTR)), defaultAppConfigElement);
    }

    private static UpdateStrategy parseUpdateStrategy(final String updateStrategyValue) {
        if (Strings.isNullOrEmpty(updateStrategyValue)
                || updateStrategyValue.equalsIgnoreCase(UpdateStrategy.RELOAD.name())) {
            return UpdateStrategy.RELOAD;
        } else if (updateStrategyValue.equalsIgnoreCase(UpdateStrategy.NONE.name())) {
            return UpdateStrategy.NONE;
        } else {
            LOG.warn("update-strategy {} not supported, using reload", updateStrategyValue);
            return UpdateStrategy.RELOAD;
        }
    }

    private static Element parseXML(final String name, final String xml) {
        try {
            return UntrustedXML.newDocumentBuilder().parse(new InputSource(new StringReader(xml))).getDocumentElement();
        } catch (SAXException | IOException e) {
            throw new ComponentDefinitionException(String.format("Error %s parsing XML: %s", name, xml), e);
        }
    }

    private static ValueMetadata createValue(final ParserContext context, final String value) {
        MutableValueMetadata metadata = context.createMetadata(MutableValueMetadata.class);
        metadata.setStringValue(value);
        return metadata;
    }

    private static MutableReferenceMetadata createServiceRef(final ParserContext context, final Class<?> cls,
            final String filter) {
        MutableReferenceMetadata metadata = context.createMetadata(MutableReferenceMetadata.class);
        metadata.setRuntimeInterface(cls);
        metadata.setInterface(cls.getName());
        metadata.setActivation(ReferenceMetadata.ACTIVATION_EAGER);
        metadata.setAvailability(ReferenceMetadata.AVAILABILITY_MANDATORY);

        if (filter != null) {
            metadata.setFilter(filter);
        }

        return metadata;
    }

    private static RefMetadata createRef(final ParserContext context, final String id) {
        MutableRefMetadata metadata = context.createMetadata(MutableRefMetadata.class);
        metadata.setComponentId(id);
        return metadata;
    }

    private static String getId(final ParserContext context, final Element element) {
        if (element.hasAttribute(ID_ATTR)) {
            return element.getAttribute(ID_ATTR);
        } else {
            return context.generateId();
        }
    }

    private static boolean nodeNameEquals(final Node node, final String name) {
        return name.equals(node.getNodeName()) || name.equals(node.getLocalName());
    }

    private static void addBlueprintBundleRefProperty(final ParserContext context, final MutableBeanMetadata metadata) {
        metadata.addProperty("bundle", createRef(context, "blueprintBundle"));
    }

    private static MutableBeanMetadata createBeanMetadata(final ParserContext context, final String id,
            final Class<?> runtimeClass, final boolean initMethod, final boolean destroyMethod) {
        MutableBeanMetadata metadata = context.createMetadata(MutableBeanMetadata.class);
        metadata.setId(id);
        metadata.setScope(BeanMetadata.SCOPE_SINGLETON);
        metadata.setActivation(ReferenceMetadata.ACTIVATION_EAGER);
        metadata.setRuntimeClass(runtimeClass);

        if (initMethod) {
            metadata.setInitMethod("init");
        }

        if (destroyMethod) {
            metadata.setDestroyMethod("destroy");
        }

        return metadata;
    }
}
