package org.opendaylight.controller.netconf.cli.writer.impl;

import java.io.IOException;
import java.util.List;

import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.writer.AbstractWriter;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;

public class LeafListWriter extends AbstractWriter<LeafListSchemaNode> {

    public LeafListWriter(ConsoleIO console, String indent) {
        super(console, indent);
    }

    @Override
    public void writeInner(LeafListSchemaNode dataSchemaNode, List<Node<?>> dataNodes) throws WriteException,
            IOException {
        StringBuilder output = new StringBuilder();
        output.append(indent);
        output.append(dataSchemaNode.getQName().getLocalName());
        output.append("=");
        String separator = "";
        for (Node<?> node : dataNodes) {
            output.append("[");
            output.append(node.getValue());
            output.append("]");
            output.append(separator);
            output.append("\n");
            separator = ",";
        }
        console.write(output.toString());
    }

}
