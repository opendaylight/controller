package org.opendaylight.controller.netconf.cli.writer.impl;

import java.io.IOException;
import java.util.List;

import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.writer.AbstractWriter;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;

import com.google.common.base.Preconditions;

public class LeafWriter extends AbstractWriter<LeafSchemaNode> {

    public LeafWriter(final ConsoleIO console, final String indent) {
        super(console, indent);
    }

    @Override
    public void writeInner(final LeafSchemaNode leafSchemaNode, final List<Node<?>> dataNode) throws WriteException,
            IOException {
        Preconditions.checkArgument(dataNode.size() == 1,
                "Unexpected number of elements for a leaf, should be 1, was %s", dataNode);

        final StringBuilder output = new StringBuilder();
        // TODO seems like this code to write "name value" is present in multiple writers, Reuse
        output.append(indent);
        output.append(leafSchemaNode.getQName().getLocalName());
        output.append(" ");
        output.append(dataNode.get(0).getValue());
        output.append("\n");
        console.write(output.toString());
    }
}
