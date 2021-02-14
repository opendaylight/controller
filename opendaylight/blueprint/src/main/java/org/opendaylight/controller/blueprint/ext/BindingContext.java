/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMSource;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaTreeInference;
import org.opendaylight.yangtools.yang.model.api.stmt.KeyEffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaTreeEffectiveStatement;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Base class to abstract binding type-specific behavior.
 *
 * @author Thomas Pantelis (originally; re-factored by Michael Vorburger.ch)
 */
public abstract class BindingContext {
    private static String GET_KEY_METHOD = "key";

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

    public NormalizedNode parseDataElement(final Element element, final SchemaTreeInference dataSchema)
            throws XMLStreamException, IOException, ParserConfigurationException, SAXException, URISyntaxException {
        final NormalizedNodeResult resultHolder = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(resultHolder);
        final XmlParserStream xmlParser = XmlParserStream.create(writer, dataSchema);
        xmlParser.traverse(new DOMSource(element));

        final NormalizedNode result = resultHolder.getResult();
        if (result instanceof MapNode) {
            final MapNode mapNode = (MapNode) result;
            final MapEntryNode mapEntryNode = mapNode.body().iterator().next();
            return mapEntryNode;
        }

        return result;
    }

    public abstract NormalizedNode newDefaultNode(SchemaTreeInference dataSchema);

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
        public NormalizedNode newDefaultNode(final SchemaTreeInference dataSchema) {
            return ImmutableNodes.containerNode(bindingQName);
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
        static ListBindingContext newInstance(final Class<? extends DataObject> bindingClass,
                final String listKeyValue) throws InstantiationException, IllegalAccessException,
                IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
            // We assume the yang list key type is string.
            Identifier keyInstance = (Identifier) bindingClass.getMethod(GET_KEY_METHOD).getReturnType()
                    .getConstructor(String.class).newInstance(listKeyValue);
            InstanceIdentifier appConfigPath = InstanceIdentifier.builder((Class)bindingClass, keyInstance).build();
            return new ListBindingContext(bindingClass, appConfigPath, listKeyValue);
        }

        @Override
        public NormalizedNode newDefaultNode(final SchemaTreeInference dataSchema) {
            final SchemaTreeEffectiveStatement<?> stmt = Iterables.getLast(dataSchema.statementPath());

            // We assume there's only one key for the list.
            final Set<QName> keys = stmt.findFirstEffectiveSubstatementArgument(KeyEffectiveStatement.class)
                .orElseThrow();

            checkArgument(keys.size() == 1, "Expected only 1 key for list %s", appConfigBindingClass);
            QName listKeyQName = keys.iterator().next();
            return ImmutableNodes.mapEntryBuilder(bindingQName, listKeyQName, appConfigListKeyValue).build();
        }
    }
}
