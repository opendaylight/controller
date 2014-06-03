package org.opendaylight.controller.netconf.cli.writer.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.writer.OutFormatter;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.base.serializer.NodeSerializerDispatcher;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.DomUtils;
import org.opendaylight.yangtools.yang.data.json.schema.cnsn.parser.CnSnToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericWriter extends AbstractWriter<DataSchemaNode> {

    private static final Logger LOG = LoggerFactory.getLogger(GenericWriter.class);
    private final OutFormatter out;

    // TODO remove indent, or change its purpose
    // Now it just indents the top level element
    public GenericWriter(final ConsoleIO console, final OutFormatter out) {
        super(console);
        this.out = out;
    }

    @Override
    // TODO dataSchemaNode as optional parameter?
    public void writeInner(final DataSchemaNode dataSchemaNode, final List<Node<?>> dataNodes) throws WriteException,
            IOException {

        // TODO - add getDispatcher method to CnSnToNormalizedNodeParserFactory
        // to be able call dispatchChildElement
//        writeDefault(console, dataNodes, "");
        // FIXME remove method removeNullNodes after https://git.opendaylight.org/gerrit/#/c/7664/ is merged
        final List<Node<?>> newNodes = removeNullNodes(dataNodes);
//        writeDefault(console, newNodes, "");

        final DataContainerChild<? extends PathArgument, ?> dataContainerChild = parseToNormalizedNode(newNodes,
                dataSchemaNode);

        if (dataContainerChild != null) {
            console.writeLn(serializeToCliOutput(dataContainerChild, dataSchemaNode));
        }

    }

    private String serializeToCliOutput(final DataContainerChild<? extends PathArgument, ?> dataContainerChild,
            final DataSchemaNode childSchema) {
        final CliOutputFromNormalizedNodeSerializerFactory factorySerialization = CliOutputFromNormalizedNodeSerializerFactory
                .getInstance(out, DomUtils.defaultValueCodecProvider());
        final NodeSerializerDispatcher<String> dispatcher = factorySerialization.getDispatcher();
        final Iterable<String> result = dispatcher.dispatchChildElement(childSchema, dataContainerChild);

        if (result == null) {
            return "";
        }

        final Iterator<String> output = result.iterator();
        if (!output.hasNext()) {
            return "";
        }

        return output.next();
    }

    // TODO move parseToNormalizedNode to AbstractWriter
    private DataContainerChild<? extends PathArgument, ?> parseToNormalizedNode(final List<Node<?>> dataNodes,
            final DataSchemaNode dataSchemaNode) {
        final CnSnToNormalizedNodeParserFactory factoryParsing = CnSnToNormalizedNodeParserFactory.getInstance();
        if (dataSchemaNode instanceof ContainerSchemaNode) {
            return factoryParsing.getContainerNodeParser().parse(dataNodes, (ContainerSchemaNode) dataSchemaNode);
        } else if (dataSchemaNode instanceof LeafSchemaNode) {
            return factoryParsing.getLeafNodeParser().parse(dataNodes, (LeafSchemaNode) dataSchemaNode);
        } else if (dataSchemaNode instanceof LeafListSchemaNode) {
            return factoryParsing.getLeafSetNodeParser().parse(dataNodes, (LeafListSchemaNode) dataSchemaNode);
        } else if (dataSchemaNode instanceof ListSchemaNode) {
            return factoryParsing.getMapNodeParser().parse(dataNodes, (ListSchemaNode) dataSchemaNode);
        } else if (dataSchemaNode instanceof ChoiceNode) {
            return factoryParsing.getChoiceNodeParser().parse(dataNodes, (ChoiceNode) dataSchemaNode);
        } else if (dataSchemaNode instanceof AugmentationSchema) {
            return factoryParsing.getAugmentationNodeParser().parse(dataNodes, (AugmentationSchema) dataSchemaNode);
        }
        return null;
    }

    // FIXME remove method removeNullNodes after https://git.opendaylight.org/gerrit/#/c/7664/ is merged
    private List<Node<?>> removeNullNodes(final List<Node<?>> dataNodes) {
        final List<Node<?>> result = new ArrayList<Node<?>>();
        for (final Node<?> dataNode : dataNodes) {
            if (dataNode instanceof CompositeNode) {
                final List<Node<?>> newChilds = removeNullNodes(((CompositeNode) dataNode).getValue());
                if (!newChilds.isEmpty()) {
                    result.add(NodeFactory.createImmutableCompositeNode(dataNode.getNodeType(), null, newChilds));
                }
            } else if (dataNode instanceof SimpleNode<?>) {
                final SimpleNode<?> simpleNode = (SimpleNode<?>) dataNode;
                if (simpleNode.getValue() != null) {
                    result.add( NodeFactory.createImmutableSimpleNode(
                            simpleNode.getNodeType(), null, simpleNode.getValue()));
                } else {
                    LOG.warn("Null value for node "+simpleNode.getNodeType().getLocalName()+".");
                }
            }
        }
        return result;
    }

}