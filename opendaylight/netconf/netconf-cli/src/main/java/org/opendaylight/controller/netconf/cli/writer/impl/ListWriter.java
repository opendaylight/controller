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

    public ListWriter(ConsoleIO console, String indent) {
        super(console, indent);
    }

    @Override
    public void writeInner(ListSchemaNode dataSchemaNode, List<Node<?>> dataNode) throws WriteException, IOException {
        StringBuilder output = new StringBuilder();
        for (Node<?> childNode : dataNode) {
            output.append(indent);
            output.append(dataSchemaNode.getQName().getLocalName());
            output.append(keyValues(dataSchemaNode, childNode));
            output.append("\n");
            console.write(output.toString());
            new StructuredDataWriter(console, indent).write(dataSchemaNode,
                    Collections.<Node<?>> singletonList(childNode));

        }
    }

    private String keyValues(ListSchemaNode dataSchemaNode, Node<?> childNode) {
        StringBuilder output = new StringBuilder();
        List<QName> keyQNames = dataSchemaNode.getKeyDefinition();
        output.append("[");
        for (QName keyQName : keyQNames) {
            if (childNode instanceof CompositeNode) {
                SimpleNode<?> firstSimpleByName = ((CompositeNode) childNode).getFirstSimpleByName(keyQName);
                if (firstSimpleByName != null) {
                    output.append(keyQName.getLocalName());
                    output.append("=");
                    output.append(firstSimpleByName.getValue());
                }
            }
        }
        output.append("]");
        return output.toString();
    }

}
