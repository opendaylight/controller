/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands.output;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

/**
 * Output values for and rpc/command execution
 */
public class Output {

    private final NormalizedNode<?, ?> output;

    public Output(final NormalizedNode<?, ?> output) {
        if (output instanceof ContainerNode && output.getNodeType().getLocalName() == "rpc-reply") {
            this.output = ((ContainerNode) output).getValue().iterator().next();
        } else {
            this.output = output;
        }
    }

    public Map<DataSchemaNode, List<NormalizedNode<?, ?>>> unwrap(final OutputDefinition outputDefinition) {
        Preconditions.checkArgument(outputDefinition.isEmpty() == false);

        final Map<QName, DataSchemaNode> mappedSchemaNodes = mapOutput(outputDefinition);
        final Map<DataSchemaNode, List<NormalizedNode<?, ?>>> mappedNodesToSchema = Maps.newHashMap();

        final DataSchemaNode schemaNode = mappedSchemaNodes.get(output.getNodeType().withoutRevision());
        final List<NormalizedNode<?, ?>> list = mappedNodesToSchema.get(schemaNode) == null ? Lists.<NormalizedNode<?, ?>>newArrayList()
                : mappedNodesToSchema.get(schemaNode);
        list.add(output);
        mappedNodesToSchema.put(schemaNode, list);

        return mappedNodesToSchema;
    }

    public NormalizedNode<?, ?> getOutput() {
        return output;
    }

    private Map<QName, DataSchemaNode> mapOutput(final OutputDefinition outputDefinition) {
        final Map<QName, DataSchemaNode> mapped = Maps.newHashMap();
        for (final DataSchemaNode dataSchemaNode : outputDefinition) {
            // without revision since data QNames come without revision
            mapped.put(dataSchemaNode.getQName().withoutRevision(), dataSchemaNode);
        }

        return mapped;
    }
}
