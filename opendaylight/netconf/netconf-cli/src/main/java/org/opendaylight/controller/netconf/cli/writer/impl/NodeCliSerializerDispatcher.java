/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.writer.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.MixinNode;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.FromNormalizedNodeSerializerFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer.NodeSerializerDispatcher;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

public class NodeCliSerializerDispatcher implements NodeSerializerDispatcher<String> {
    private final FromNormalizedNodeSerializerFactory<String> factory;

    public NodeCliSerializerDispatcher(final FromNormalizedNodeSerializerFactory<String> factory) {
        this.factory = Preconditions.checkNotNull(factory);
    }

    @Override
    public final Iterable<String> dispatchChildElement(final Object childSchema,
            final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> dataContainerChild) {
        if (dataContainerChild instanceof ContainerNode) {
            return onContainerNode(childSchema, dataContainerChild);
        } else if (dataContainerChild instanceof LeafNode<?>) {
            return onLeafNode(childSchema, dataContainerChild);
        } else if (dataContainerChild instanceof MixinNode) {
            if (dataContainerChild instanceof LeafSetNode<?>) {
                return onLeafListNode(childSchema, dataContainerChild);
            } else if (dataContainerChild instanceof MapNode) {
                return onListNode(childSchema, dataContainerChild);
            } else if (dataContainerChild instanceof ChoiceNode) {
                return onChoiceNode(childSchema, dataContainerChild);
            } else if (dataContainerChild instanceof AugmentationNode) {
                return onAugmentationSchema(childSchema, dataContainerChild);
            }
        }
        throw new IllegalArgumentException("Unable to serialize " + childSchema);
    }

    private Iterable<String> onAugmentationSchema(final Object childSchema,
            final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> dataContainerChild) {
        checkSchemaCompatibility(childSchema, AugmentationSchema.class, dataContainerChild);
        return factory.getAugmentationNodeSerializer().serialize((AugmentationSchema) childSchema,
                (AugmentationNode) dataContainerChild);
    }

    private Iterable<String> onChoiceNode(final Object childSchema,
            final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> dataContainerChild) {
        checkSchemaCompatibility(childSchema, ChoiceSchemaNode.class, dataContainerChild);
        return factory.getChoiceNodeSerializer().serialize(
                (ChoiceSchemaNode) childSchema, (ChoiceNode) dataContainerChild);
    }

    private Iterable<String> onListNode(final Object childSchema,
            final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> dataContainerChild) {
        checkSchemaCompatibility(childSchema, ListSchemaNode.class, dataContainerChild);
        return factory.getMapNodeSerializer().serialize((ListSchemaNode) childSchema, (MapNode) dataContainerChild);
    }

    private Iterable<String> onLeafListNode(final Object childSchema,
            final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> dataContainerChild) {
        checkSchemaCompatibility(childSchema, LeafListSchemaNode.class, dataContainerChild);
        return factory.getLeafSetNodeSerializer().serialize((LeafListSchemaNode) childSchema,
                (LeafSetNode<?>) dataContainerChild);
    }

    private Iterable<String> onLeafNode(final Object childSchema,
            final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> dataContainerChild) {
        checkSchemaCompatibility(childSchema, LeafSchemaNode.class, dataContainerChild);
        final Iterable<String> elements = factory.getLeafNodeSerializer().serialize((LeafSchemaNode) childSchema,
                (LeafNode<?>) dataContainerChild);
        checkOnlyOneSerializedElement(elements, dataContainerChild);
        return elements;
    }

    private static void checkOnlyOneSerializedElement(final Iterable<?> elements,
            final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> dataContainerChild) {
        final int size = Iterables.size(elements);
        Preconditions.checkArgument(size == 1,
                "Unexpected count of elements for entry serialized from: %s, should be 1, was: %s", dataContainerChild,
                size);
    }

    private Iterable<String> onContainerNode(final Object childSchema,
            final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> dataContainerChild) {
        checkSchemaCompatibility(childSchema, ContainerSchemaNode.class, dataContainerChild);

        final Iterable<String> elements = factory.getContainerNodeSerializer().serialize(
                (ContainerSchemaNode) childSchema, (ContainerNode) dataContainerChild);
        checkOnlyOneSerializedElement(elements, dataContainerChild);
        return elements;
    }

    private static void checkSchemaCompatibility(final Object childSchema, final Class<?> containerSchemaNodeClass,
            final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> dataContainerChild) {
        Preconditions.checkArgument(containerSchemaNodeClass.isAssignableFrom(childSchema.getClass()),
                "Incompatible schema: %s with node: %s, expected: %s", childSchema, dataContainerChild,
                containerSchemaNodeClass);
    }
}
