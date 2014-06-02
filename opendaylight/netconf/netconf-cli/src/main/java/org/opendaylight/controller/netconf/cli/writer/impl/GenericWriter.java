package org.opendaylight.controller.netconf.cli.writer.impl;

import java.io.IOException;
import java.util.List;

import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.writer.AbstractWriter;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

public class GenericWriter extends AbstractWriter<DataSchemaNode> {

    // TODO remove indent, or change its purpose
    // Now it just indents the top level element
    public GenericWriter(ConsoleIO console, String indent) {
        super(console, indent);
    }

    @Override
    // TODO dataSchemaNode as optional parameter?
    public void writeInner(final DataSchemaNode dataSchemaNode, final List<Node<?>> dataNodes) throws WriteException,
            IOException {
        if (dataSchemaNode instanceof ContainerSchemaNode) {
            new ContainerWriter(console, indent).write((ContainerSchemaNode) dataSchemaNode, dataNodes);
        } else if (dataSchemaNode instanceof ListSchemaNode) {
            new ListWriter(console, indent).write((ListSchemaNode) dataSchemaNode, dataNodes);
        } else if (dataSchemaNode instanceof LeafListSchemaNode) {
            new LeafListWriter(console, indent).write((LeafListSchemaNode) dataSchemaNode, dataNodes);
        } else if (dataSchemaNode instanceof LeafSchemaNode) {
            new LeafWriter(console, indent).write((LeafSchemaNode) dataSchemaNode, dataNodes);
        } else {
            writeDefault(console, dataNodes, indent);
        }
    }

    private void writeDefault(final ConsoleIO console, final List<Node<?>> dataNodes, final String indent)
            throws IOException {
        for (final Node<?> dataNode : dataNodes) {
            final StringBuilder output = new StringBuilder();
            if (dataNode instanceof CompositeNode) {
                // TODO this is the same as ContainerWriter
                output.append(indent);
                output.append(dataNode.getNodeType().getLocalName());
                output.append(OUTPUT_OPEN_NODE);
                output.append("\n");
                console.write(output.toString());
                writeDefault(console, ((CompositeNode) dataNode).getValue(), indent + indent());
                console.write(indent);
                console.writeLn(OUTPUT_CLOSE_NODE);
            } else if (dataNode instanceof SimpleNode<?>) {
                final SimpleNode<?> simpleNode = (SimpleNode<?>) dataNode;
                output.append(indent);
                output.append(simpleNode.getNodeType().getLocalName());
                output.append(" ");
                output.append(simpleNode.getValue());
                output.append("\n");
                console.write(output.toString());
            }
        }

    }

}
