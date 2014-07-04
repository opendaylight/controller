/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.writer.impl;

import java.io.IOException;
import java.util.List;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.writer.OutFormatter;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public class CompositeNodeWriter extends AbstractWriter<DataSchemaNode> {

    private final OutFormatter outFormatter;

    public CompositeNodeWriter(final ConsoleIO console, final OutFormatter outFormatter) {
        super(console);
        this.outFormatter = outFormatter;
    }

    @Override
    protected void writeInner(final DataSchemaNode dataSchemaNode, final List<Node<?>> dataNodes) throws IOException, WriteException {
        final StringBuilder output = new StringBuilder();
        writeNode(dataNodes, output);
        console.writeLn(output);
    }

    private void writeNode(final List<Node<?>> dataNodes, final StringBuilder output) throws IOException, WriteException {
        for (final Node<?> dataNode : dataNodes) {
            outFormatter.increaseIndent();
            outFormatter.addStringWithIndent(output, dataNode.getNodeType().getLocalName());
            if (dataNode instanceof CompositeNode) {
                outFormatter.openComposite(output);
                outFormatter.newLine(output);
                writeNode(((CompositeNode) dataNode).getValue(), output);
                outFormatter.closeCompositeWithIndent(output);
                outFormatter.newLine(output);
            } else if (dataNode instanceof SimpleNode<?>) {
                final SimpleNode<?> simpleNode = (SimpleNode<?>) dataNode;
                output.append(" ");
                output.append(simpleNode.getValue());
                outFormatter.newLine(output);
            }
            outFormatter.decreaseIndent();
        }
    }
}
