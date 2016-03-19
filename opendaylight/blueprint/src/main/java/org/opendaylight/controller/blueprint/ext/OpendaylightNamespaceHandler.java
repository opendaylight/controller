/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import java.net.URL;
import java.util.Collections;
import java.util.Set;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableReferenceMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.opendaylight.controller.blueprint.BlueprintContainerRestartService;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Thomas Pantelis
 */
public class OpendaylightNamespaceHandler implements NamespaceHandler {
    public static final String NAMESPACE_1_0_0 = "http://opendaylight.org/xmlns/blueprint/v1.0.0";

    private static final Logger LOG = LoggerFactory.getLogger(OpendaylightNamespaceHandler.class);
    private static final String PROCESSOR = "processor";
    private static final String RESTART_DEPENDENTS = "restart-dependents-on-update";

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

        if (nodeNameEquals(element, PROCESSOR)) {
            return parseProcessor(element, context);
        }

        throw new ComponentDefinitionException("Unsupported standalone element: " + element.getNodeName());
    }

    @Override
    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        // No decorating elements
//        if(node instanceof Element) {
//            if (nodeNameEquals(node, RESTART_DEPENDENTS)) {
//                decorateRestartDependents((Element)node, component);
//            } else {
//                throw new ComponentDefinitionException("Unsupported standalone element: " + node.getNodeName());
//            }
//        } else {
//            throw new ComponentDefinitionException("Invalid node type " + node);
//        }

        return null;
    }

    private static Metadata parseProcessor(Element element, ParserContext context) {
        MutableBeanMetadata metadata = context.createMetadata(MutableBeanMetadata.class);
        metadata.setProcessor(true);
        metadata.setId(context.generateId());
        metadata.setScope(BeanMetadata.SCOPE_SINGLETON);
        metadata.setRuntimeClass(ComponentProcessor.class);
        metadata.setDestroyMethod("destroy");
        metadata.addProperty("bundle", createRef(context, "blueprintBundle"));
        metadata.addProperty("blueprintContainerRestartService", createServiceRef(context,
                BlueprintContainerRestartService.class, null));

        String restartDependentsOnUpdate = "true";
        if(element.hasAttribute(RESTART_DEPENDENTS)) {
            restartDependentsOnUpdate = element.getAttribute(RESTART_DEPENDENTS);
        }

        metadata.addProperty("restartDependentsOnUpdate", createValue(context, restartDependentsOnUpdate));

        return metadata;
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

    private static RefMetadata createRef(ParserContext context, String value) {
        MutableRefMetadata metadata = context.createMetadata(MutableRefMetadata.class);
        metadata.setComponentId(value);
        return metadata;
    }

    private static boolean nodeNameEquals(Node node, String name) {
        return name.equals(node.getNodeName()) || name.equals(node.getLocalName());
    }
}
