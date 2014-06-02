package org.opendaylight.controller.netconf.cli.writer.impl;

import java.io.IOException;
import java.util.List;

import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.writer.AbstractWriter;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;

public class LeafListWriter extends AbstractWriter<LeafListSchemaNode> {

    public LeafListWriter(final ConsoleIO console, final String indent) {
        super(console, indent);
    }

    @Override
    public void writeInner(final LeafListSchemaNode dataSchemaNode, final List<Node<?>> dataNodes)
            throws WriteException, IOException {
        final StringBuilder output = new StringBuilder();
        output.append(indent);
        output.append(dataSchemaNode.getQName().getLocalName());
        output.append(" ");
        String separator = "";
        output.append("[");
        for (final Node<?> node : dataNodes) {
            output.append(separator);
            output.append(node.getValue());
            separator = ",";
        }
        output.append("]");
        output.append("\n");
        console.write(output.toString());
    }
}
