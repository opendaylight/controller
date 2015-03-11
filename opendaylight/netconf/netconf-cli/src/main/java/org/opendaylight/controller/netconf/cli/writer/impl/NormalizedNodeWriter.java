/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.writer.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.writer.OutFormatter;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.composite.node.schema.cnsn.parser.CnSnToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer.NodeSerializerDispatcher;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.DomUtils;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NormalizedNodeWriter extends AbstractWriter<DataSchemaNode> {

    private static final Logger LOG = LoggerFactory.getLogger(NormalizedNodeWriter.class);
    private final OutFormatter out;

    public NormalizedNodeWriter(final ConsoleIO console, final OutFormatter out) {
        super(console);
        this.out = out;
    }

    public void writeInner(final DataSchemaNode dataSchemaNode, final List<Node<?>> dataNodes) throws WriteException,
            IOException {

        // TODO - add getDispatcher method to CnSnToNormalizedNodeParserFactory
        // to be able call dispatchChildElement
        final DataContainerChild<? extends PathArgument, ?> dataContainerChild = parseToNormalizedNode(dataNodes,
                dataSchemaNode);

        if (dataContainerChild != null) {
            console.writeLn(serializeToCliOutput(dataContainerChild, dataSchemaNode));
        }

    }

    private String serializeToCliOutput(final DataContainerChild<? extends PathArgument, ?> dataContainerChild,
            final DataSchemaNode childSchema) {
        final CliOutputFromNormalizedNodeSerializerFactory factorySerialization = CliOutputFromNormalizedNodeSerializerFactory
                .getInstance(out, DomUtils.defaultValueCodecProvider());
        final NodeSerializerDispatcher<String> dispatcher = factorySerialization.getDispatcher();
        final Iterable<String> result = dispatcher.dispatchChildElement(childSchema, dataContainerChild);

        if (result == null) {
            return "";
        }

        final Iterator<String> output = result.iterator();
        if (!output.hasNext()) {
            return "";
        }

        return output.next();
    }

    private DataContainerChild<? extends PathArgument, ?> parseToNormalizedNode(final List<Node<?>> dataNodes,
            final DataSchemaNode dataSchemaNode) {
        final CnSnToNormalizedNodeParserFactory factoryParsing = CnSnToNormalizedNodeParserFactory.getInstance();
        if (dataSchemaNode instanceof ContainerSchemaNode) {
            return factoryParsing.getContainerNodeParser().parse(dataNodes, (ContainerSchemaNode) dataSchemaNode);
        } else if (dataSchemaNode instanceof LeafSchemaNode) {
            return factoryParsing.getLeafNodeParser().parse(dataNodes, (LeafSchemaNode) dataSchemaNode);
        } else if (dataSchemaNode instanceof LeafListSchemaNode) {
            return factoryParsing.getLeafSetNodeParser().parse(dataNodes, (LeafListSchemaNode) dataSchemaNode);
        } else if (dataSchemaNode instanceof ListSchemaNode) {
            return factoryParsing.getMapNodeParser().parse(dataNodes, (ListSchemaNode) dataSchemaNode);
        } else if (dataSchemaNode instanceof ChoiceSchemaNode) {
            return factoryParsing.getChoiceNodeParser().parse(dataNodes, (ChoiceSchemaNode) dataSchemaNode);
        } else if (dataSchemaNode instanceof AugmentationSchema) {
            return factoryParsing.getAugmentationNodeParser().parse(dataNodes, (AugmentationSchema) dataSchemaNode);
        }
        return null;
    }

}
