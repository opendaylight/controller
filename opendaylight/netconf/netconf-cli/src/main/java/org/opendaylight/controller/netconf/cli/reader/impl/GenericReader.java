package org.opendaylight.controller.netconf.cli.reader.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

public class GenericReader extends AbstractReader<DataSchemaNode> {

    public GenericReader(final ConsoleIO console) {
        super(console);
    }

    @Override
    protected List<Node<?>> readInner(final DataSchemaNode schemaNode) throws IOException, ReadingException {
        // TODO reuse instances
        final List<Node<?>> newNodes = new ArrayList<>();
        if (schemaNode instanceof LeafSchemaNode) {
            return new LeafReader(console).read((LeafSchemaNode) schemaNode);
        } else if (schemaNode instanceof ContainerSchemaNode) {
            return new ContainerReader(console).read((ContainerSchemaNode) schemaNode);
        } else if (schemaNode instanceof ListSchemaNode) {
            return new GenericListReader<>(console, new ListEntryReader(console)).read((ListSchemaNode) schemaNode);
        } else if (schemaNode instanceof LeafListSchemaNode) {
            return new GenericListReader<>(console, new LeafListEntryReader(console))
                    .read((LeafListSchemaNode) schemaNode);
        } else if (schemaNode instanceof ChoiceNode) {
            return new ChoiceReader(console).read((ChoiceNode) schemaNode);
        } else if (schemaNode instanceof AnyXmlSchemaNode) {
            return new AnyXmlReader(console).read((AnyXmlSchemaNode) schemaNode);
        }
        return newNodes;
    }

    public List<Node<?>> read(final Iterable<DataSchemaNode> nodes) throws ReadingException {
        final List<Node<?>> newNodes = new ArrayList<>();
        for (final DataSchemaNode schemaNode : nodes) {
            newNodes.addAll(read(schemaNode));
        }
        return newNodes;
    }

    @Override
    protected ConsoleContext getContext(final DataSchemaNode schemaNode) {
        return new BaseConsoleContext<DataSchemaNode>();
    }
}
