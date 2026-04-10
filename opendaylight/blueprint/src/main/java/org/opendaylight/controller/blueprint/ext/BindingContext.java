/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMSource;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.binding.ChildOf;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataRoot;
import org.opendaylight.yangtools.binding.EntryObject;
import org.opendaylight.yangtools.binding.Key;
import org.opendaylight.yangtools.binding.contract.Naming;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaTreeInference;
import org.opendaylight.yangtools.yang.model.api.stmt.KeyEffectiveStatement;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Base class to abstract binding type-specific behavior.
 *
 * @author Thomas Pantelis (originally; re-factored by Michael Vorburger.ch)
 */
abstract sealed class BindingContext {
    /**
     * BindingContext implementation for a container binding.
     */
    private static final class ContainerBindingContext extends BindingContext {
        @SuppressWarnings("unchecked")
        ContainerBindingContext(final BindingClass<?> appConfigBindingClass) {
            super(appConfigBindingClass, DataObjectIdentifier.builder((Class) appConfigBindingClass.clazz()).build(),
                ContainerSchemaNode.class);
        }

        @Override
        public ContainerNode newDefaultNode(final SchemaTreeInference dataSchema) {
            return ImmutableNodes.newContainerBuilder().withNodeIdentifier(new NodeIdentifier(qname())).build();
        }
    }

    /**
     * BindingContext implementation for a list binding.
     */
    private static final class ListBindingContext extends BindingContext {
        private final String appConfigListKeyValue;

        @SuppressWarnings("unchecked")
        private ListBindingContext(final BindingClass<?> appConfigBindingClass,
                final DataObjectIdentifier<?> appConfigPath, final String appConfigListKeyValue) {
            super(appConfigBindingClass, (DataObjectIdentifier<DataObject>) appConfigPath, ListSchemaNode.class);
            this.appConfigListKeyValue = appConfigListKeyValue;
        }

        static <T extends EntryObject<T, K> & ChildOf<? extends DataRoot<?>>, K extends Key<T>>
                ListBindingContext newInstance(final BindingClass<T> bindingClass, final String listKeyValue)
                    throws InstantiationException, IllegalAccessException, IllegalArgumentException,
                           InvocationTargetException, NoSuchMethodException, SecurityException {
            // We assume the YANG list key type is string.
            @SuppressWarnings("unchecked")
            final var keyInstance = (K) bindingClass.clazz().getMethod(Naming.KEY_AWARE_KEY_NAME)
                .getReturnType().getConstructor(String.class).newInstance(listKeyValue);

            return new ListBindingContext(bindingClass,
                DataObjectIdentifier.builder(bindingClass.clazz(), keyInstance).build(), listKeyValue);
        }

        @Override
        NormalizedNode newDefaultNode(final SchemaTreeInference dataSchema) {
            final var stmt = dataSchema.statementPath().getLast();

            // We assume there's only one key for the list.
            final var keys = stmt.findFirstEffectiveSubstatementArgument(KeyEffectiveStatement.class).orElseThrow();
            if (keys.size() != 1) {
                throw new IllegalArgumentException("Expected only 1 key for list " + bindingClass());
            }

            QName listKeyQName = keys.iterator().next();
            return ImmutableNodes.newMapEntryBuilder()
                .withNodeIdentifier(NodeIdentifierWithPredicates.of(qname(), listKeyQName, appConfigListKeyValue))
                .withChild(ImmutableNodes.leafNode(listKeyQName, appConfigListKeyValue))
                .build();
        }
    }

    private final BindingClass<?> appConfigBindingClass;
    final DataObjectIdentifier<DataObject> appConfigPath;
    final Class<? extends DataSchemaNode> schemaType;

    private BindingContext(final BindingClass<?> appConfigBindingClass,
            final DataObjectIdentifier<DataObject> appConfigPath, final Class<? extends DataSchemaNode> schemaType) {
        this.appConfigBindingClass = appConfigBindingClass;
        this.appConfigPath = appConfigPath;
        this.schemaType = schemaType;
    }

    static BindingContext create(final String logName, final Class<? extends DataObject> klass,
            final String appConfigListKeyValue) {
        final var bindingClass = BindingClass.of(logName, klass);
        if (!EntryObject.class.isAssignableFrom(klass)) {
            return new ContainerBindingContext(bindingClass);
        }

        // The binding class corresponds to a yang list.
        // FIXME: support empty keys?
        if (appConfigListKeyValue == null || appConfigListKeyValue.isEmpty()) {
            throw new ComponentDefinitionException(String.format(
                "%s: App config binding class %s represents a yang list therefore \"%s\" must be specified",
                logName, klass.getName(), DataStoreAppConfigMetadata.LIST_KEY_VALUE));
        }

        try {
            return ListBindingContext.newInstance((BindingClass) bindingClass, appConfigListKeyValue);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
            | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new ComponentDefinitionException(String.format(
                "%s: Error initializing for app config list binding class %s",
                logName, klass.getName()), e);
        }
    }

    final @NonNull Class<? extends DataObject> bindingClass() {
        return appConfigBindingClass.clazz();
    }

    final @NonNull QName qname() {
        return appConfigBindingClass.qname();
    }

    NormalizedNode parseDataElement(final Element element, final SchemaTreeInference dataSchema)
            throws XMLStreamException, IOException, SAXException, URISyntaxException {
        final var resultHolder = new NormalizationResultHolder();
        final var writer = ImmutableNormalizedNodeStreamWriter.from(resultHolder);
        final var xmlParser = XmlParserStream.create(writer, dataSchema);
        xmlParser.traverse(new DOMSource(element));

        final var result = resultHolder.getResult().data();
        return result instanceof MapNode mapNode ? mapNode.body().iterator().next() : result;
    }

    abstract NormalizedNode newDefaultNode(SchemaTreeInference dataSchema);
}
