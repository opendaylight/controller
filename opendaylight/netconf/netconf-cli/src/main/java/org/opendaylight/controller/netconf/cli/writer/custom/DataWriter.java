/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.writer.custom;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.writer.AbstractWriter;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.controller.netconf.cli.writer.impl.GenericWriter;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlDocumentUtils;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class DataWriter extends AbstractWriter<DataSchemaNode> {

    private final SchemaContext remoteSchemaContext;

    public DataWriter(final ConsoleIO console, final String indent, final SchemaContext remoteSchemaContext) {
        super(console, indent);
        this.remoteSchemaContext = remoteSchemaContext;
    }

    @Override
    protected void writeInner(final DataSchemaNode dataSchemaNode, final List<Node<?>> dataNodes) throws IOException, WriteException {
        Preconditions.checkArgument(dataNodes.size() == 1, "Expected only 1 element for data node");
        final Node<?> dataNode = dataNodes.get(0);
        Preconditions.checkArgument(dataNode instanceof CompositeNode);

        final StringBuilder output = new StringBuilder();
        output.append(indent);
        output.append(dataSchemaNode.getQName().getLocalName());
        output.append(OUTPUT_OPEN_NODE);
        console.writeLn(output.toString());

        for (final Node<?> childNode : ((CompositeNode) dataNode).getValue()) {
            final Optional<DataSchemaNode> schemaNode = XmlDocumentUtils.findFirstSchema(childNode.getNodeType(), remoteSchemaContext.getDataDefinitions());
            Preconditions.checkState(schemaNode.isPresent(), "Unknown data node %s, not defined in schema", childNode.getNodeType());
            // TODO list entries ?
            // FIXME We need to move to normalized nodes in order to avoid special handling for lists, choices etc.
            new GenericWriter(console, indent).write(schemaNode.get(), Collections.<Node<?>>singletonList(childNode));
        }

        console.write(indent);
        console.writeLn(OUTPUT_CLOSE_NODE);
    }
}
