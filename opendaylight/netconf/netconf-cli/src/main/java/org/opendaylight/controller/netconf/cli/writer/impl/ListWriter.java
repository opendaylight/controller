package org.opendaylight.controller.netconf.cli.writer.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.writer.AbstractWriter;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

public class ListWriter extends AbstractWriter<ListSchemaNode> {

    public ListWriter(final ConsoleIO console, final String indent) {
        super(console, indent);
    }

    @Override
    public void writeInner(final ListSchemaNode dataSchemaNode, final List<Node<?>> dataNode) throws WriteException,
            IOException {
        for (final Node<?> childNode : dataNode) {
            final StringBuilder output = new StringBuilder();
            output.append(indent);
            output.append(dataSchemaNode.getQName().getLocalName());
            output.append(keyValues(dataSchemaNode, childNode));
            output.append(OUTPUT_OPEN_NODE);
            output.append("\n");
            console.write(output.toString());
            new StructuredDataWriter(console, indent).write(dataSchemaNode,
                    Collections.<Node<?>> singletonList(childNode));
            console.write(indent);
            console.writeLn(OUTPUT_CLOSE_NODE);
        }
    }

    private String keyValues(final ListSchemaNode dataSchemaNode, final Node<?> childNode) {
        final StringBuilder output = new StringBuilder();
        final List<QName> keyQNames = dataSchemaNode.getKeyDefinition();
        output.append("[");
        for (final QName keyQName : keyQNames) {
            if (childNode instanceof CompositeNode) {
                final SimpleNode<?> firstSimpleByName = ((CompositeNode) childNode).getFirstSimpleByName(keyQName);
                if (firstSimpleByName != null) {
                    output.append(keyQName.getLocalName());
                    output.append(" ");
                    output.append(firstSimpleByName.getValue());
                }
            }
        }
        output.append("]");
        return output.toString();
    }

}
