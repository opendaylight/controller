/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.writer.impl;

import org.opendaylight.controller.netconf.cli.writer.OutFormatter;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlCodecProvider;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.FromNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.FromNormalizedNodeSerializerFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer.NodeSerializerDispatcher;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

public final class CliOutputFromNormalizedNodeSerializerFactory implements FromNormalizedNodeSerializerFactory<String> {
    private final ContainerNodeCliSerializer containerSerializer;
    private final ChoiceNodeCliSerializer choiceSerializer;
    private final AugmentationNodeCliSerializer augmentSerializer;
    private final LeafNodeCliSerializer leafNodeSerializer;
    private final LeafSetNodeCliSerializer leafSetSerializer;
    private final MapNodeCliSerializer mapNodeSerializer;
    private final LeafSetEntryNodeCliSerializer leafSetEntryNodeSerializer;
    private final MapEntryNodeCliSerializer mapEntryNodeSerializer;
    final NodeSerializerDispatcher<String> dispatcher = new NodeCliSerializerDispatcher(this);

    private CliOutputFromNormalizedNodeSerializerFactory(final OutFormatter out, final XmlCodecProvider codecProvider) {

        containerSerializer = new ContainerNodeCliSerializer(out, dispatcher);
        choiceSerializer = new ChoiceNodeCliSerializer(out, dispatcher);
        augmentSerializer = new AugmentationNodeCliSerializer(out, dispatcher);
        leafNodeSerializer = new LeafNodeCliSerializer(out);

        leafSetEntryNodeSerializer = new LeafSetEntryNodeCliSerializer(out);
        leafSetSerializer = new LeafSetNodeCliSerializer(out, leafSetEntryNodeSerializer);

        mapEntryNodeSerializer = new MapEntryNodeCliSerializer(out, dispatcher);
        mapNodeSerializer = new MapNodeCliSerializer(out, mapEntryNodeSerializer);
    }

    public NodeSerializerDispatcher<String> getDispatcher() {
        return dispatcher;
    }

    public static CliOutputFromNormalizedNodeSerializerFactory getInstance(final OutFormatter out,
            final XmlCodecProvider codecProvider) {
        return new CliOutputFromNormalizedNodeSerializerFactory(out, codecProvider);
    }

    @Override
    public FromNormalizedNodeSerializer<String, AugmentationNode, AugmentationSchema> getAugmentationNodeSerializer() {
        return augmentSerializer;
    }

    @Override
    public FromNormalizedNodeSerializer<String, ChoiceNode, ChoiceSchemaNode> getChoiceNodeSerializer() {
        return choiceSerializer;
    }

    @Override
    public FromNormalizedNodeSerializer<String, ContainerNode, ContainerSchemaNode> getContainerNodeSerializer() {
        return containerSerializer;
    }

    @Override
    public FromNormalizedNodeSerializer<String, LeafNode<?>, LeafSchemaNode> getLeafNodeSerializer() {
        return leafNodeSerializer;
    }

    @Override
    public FromNormalizedNodeSerializer<String, LeafSetEntryNode<?>, LeafListSchemaNode> getLeafSetEntryNodeSerializer() {
        return leafSetEntryNodeSerializer;
    }

    @Override
    public FromNormalizedNodeSerializer<String, LeafSetNode<?>, LeafListSchemaNode> getLeafSetNodeSerializer() {
        return leafSetSerializer;
    }

    @Override
    public FromNormalizedNodeSerializer<String, MapEntryNode, ListSchemaNode> getMapEntryNodeSerializer() {
        return mapEntryNodeSerializer;
    }

    @Override
    public FromNormalizedNodeSerializer<String, UnkeyedListNode, ListSchemaNode> getUnkeyedListNodeSerializer() {
        return null;
    }

    @Override
    public FromNormalizedNodeSerializer<String, MapNode, ListSchemaNode> getMapNodeSerializer() {
        return mapNodeSerializer;
    }

    @Override
    public FromNormalizedNodeSerializer<String, AnyXmlNode, AnyXmlSchemaNode> getAnyXmlNodeSerializer() {
        throw new UnsupportedOperationException();
    }

}
