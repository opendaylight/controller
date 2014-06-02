package org.opendaylight.controller.netconf.cli.reader.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.netconf.cli.io.BaseConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.AbstractReader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public class ContainerReader extends AbstractReader<ContainerSchemaNode> {

    public ContainerReader(final ConsoleIO console) {
        super(console);
    }

    @Override
    public List<Node<?>> readInner(final ContainerSchemaNode containerNode) throws IOException, ReadingException {
        final CompositeNodeBuilder<ImmutableCompositeNode> compositeNodeBuilder = ImmutableCompositeNode.builder();
        compositeNodeBuilder.setQName(containerNode.getQName());
        for (final DataSchemaNode childNode : containerNode.getChildNodes()) {
            compositeNodeBuilder.addAll(new GenericReader(console).read(childNode));
        }
        return Collections.<Node<?>> singletonList(compositeNodeBuilder.toInstance());
    }

    @Override
    protected ConsoleContext getContext(final ContainerSchemaNode schemaNode) {
        return new BaseConsoleContext<ContainerSchemaNode>(schemaNode);
    }
}
