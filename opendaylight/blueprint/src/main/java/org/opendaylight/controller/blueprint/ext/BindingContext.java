/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.parser.DomToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.w3c.dom.Element;

/**
 * Base class to abstract binding type-specific behavior.
 *
 * @author Thomas Pantelis (originally; re-factored by Michael Vorburger.ch)
 */
public abstract class BindingContext {

    public static BindingContext create(final String logName, final Class<? extends DataObject> klass,
            final String appConfigListKeyValue) {
        if (Identifiable.class.isAssignableFrom(klass)) {
            // The binding class corresponds to a yang list.
            if (Strings.isNullOrEmpty(appConfigListKeyValue)) {
                throw new ComponentDefinitionException(String.format(
                        "%s: App config binding class %s represents a yang list therefore \"%s\" must be specified",
                        logName, klass.getName(), DataStoreAppConfigMetadata.LIST_KEY_VALUE));
            }

            try {
                return ListBindingContext.newInstance(klass, appConfigListKeyValue);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                throw new ComponentDefinitionException(String.format(
                        "%s: Error initializing for app config list binding class %s",
                        logName, klass.getName()), e);
            }

        } else {
            return new ContainerBindingContext(klass);
        }
    }

    public final InstanceIdentifier<DataObject> appConfigPath;
    public final Class<DataObject> appConfigBindingClass;
    public final Class<? extends DataSchemaNode> schemaType;
    public final QName bindingQName;

    private BindingContext(final Class<DataObject> appConfigBindingClass,
            final InstanceIdentifier<DataObject> appConfigPath, final Class<? extends DataSchemaNode> schemaType) {
        this.appConfigBindingClass = appConfigBindingClass;
        this.appConfigPath = appConfigPath;
        this.schemaType = schemaType;

        bindingQName = BindingReflections.findQName(appConfigBindingClass);
    }

    public abstract NormalizedNode<?, ?> parseDataElement(Element element, DataSchemaNode dataSchema,
            DomToNormalizedNodeParserFactory parserFactory);

    public abstract NormalizedNode<?, ?> newDefaultNode(DataSchemaNode dataSchema);

    /**
     * BindingContext implementation for a container binding.
     */
    private static class ContainerBindingContext extends BindingContext {
        @SuppressWarnings("unchecked")
        ContainerBindingContext(final Class<? extends DataObject> appConfigBindingClass) {
            super((Class<DataObject>) appConfigBindingClass,
                    InstanceIdentifier.create((Class<DataObject>) appConfigBindingClass), ContainerSchemaNode.class);
        }

        @Override
        public NormalizedNode<?, ?> newDefaultNode(final DataSchemaNode dataSchema) {
            return ImmutableNodes.containerNode(bindingQName);
        }

        @Override
        public NormalizedNode<?, ?> parseDataElement(final Element element, final DataSchemaNode dataSchema,
                final DomToNormalizedNodeParserFactory parserFactory) {
            return parserFactory.getContainerNodeParser().parse(Collections.singletonList(element),
                    (ContainerSchemaNode)dataSchema);
        }
    }

    /**
     * BindingContext implementation for a list binding.
     */
    private static class ListBindingContext extends BindingContext {
        final String appConfigListKeyValue;

        @SuppressWarnings("unchecked")
        ListBindingContext(final Class<? extends DataObject> appConfigBindingClass,
                final InstanceIdentifier<? extends DataObject> appConfigPath, final String appConfigListKeyValue) {
            super((Class<DataObject>) appConfigBindingClass, (InstanceIdentifier<DataObject>) appConfigPath,
                    ListSchemaNode.class);
            this.appConfigListKeyValue = appConfigListKeyValue;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static ListBindingContext newInstance(final Class<? extends DataObject> bindingClass,
                final String listKeyValue) throws InstantiationException, IllegalAccessException,
                IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
            // We assume the yang list key type is string.
            Identifier keyInstance = (Identifier) bindingClass.getMethod("getKey").getReturnType()
                    .getConstructor(String.class).newInstance(listKeyValue);
            InstanceIdentifier appConfigPath = InstanceIdentifier.builder((Class)bindingClass, keyInstance).build();
            return new ListBindingContext(bindingClass, appConfigPath, listKeyValue);
        }

        @Override
        public NormalizedNode<?, ?> newDefaultNode(final DataSchemaNode dataSchema) {
            // We assume there's only one key for the list.
            List<QName> keys = ((ListSchemaNode)dataSchema).getKeyDefinition();
            Preconditions.checkArgument(keys.size() == 1, "Expected only 1 key for list %s", appConfigBindingClass);
            QName listKeyQName = keys.get(0);
            return ImmutableNodes.mapEntryBuilder(bindingQName, listKeyQName, appConfigListKeyValue).build();
        }

        @Override
        public NormalizedNode<?, ?> parseDataElement(final Element element, final DataSchemaNode dataSchema,
                final DomToNormalizedNodeParserFactory parserFactory) {
            return parserFactory.getMapEntryNodeParser().parse(Collections.singletonList(element),
                    (ListSchemaNode)dataSchema);
        }
    }
}
