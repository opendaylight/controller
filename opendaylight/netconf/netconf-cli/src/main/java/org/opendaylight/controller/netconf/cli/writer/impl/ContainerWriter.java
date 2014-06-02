package org.opendaylight.controller.netconf.cli.writer.impl;

import java.io.IOException;
import java.util.List;

import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.writer.AbstractWriter;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;

public class ContainerWriter extends AbstractWriter<ContainerSchemaNode> {

    public ContainerWriter(final ConsoleIO console, final String indent) {
        super(console, indent);
    }

    @Override
    public void writeInner(final ContainerSchemaNode dataSchemaNode, final List<Node<?>> dataNode)
            throws WriteException, IOException {
        final StringBuilder output = new StringBuilder();
        output.append(indent);
        output.append(dataSchemaNode.getQName().getLocalName());
        output.append(OUTPUT_OPEN_NODE);
        console.writeLn(output.toString());
        new StructuredDataWriter(console, indent).write(dataSchemaNode, dataNode);
        console.write(indent);
        console.writeLn(OUTPUT_CLOSE_NODE);
    }
}
