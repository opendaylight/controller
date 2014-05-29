package org.opendaylight.controller.netconf.cli.writer.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.writer.AbstractWriter;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public class StructuredDataWriter extends AbstractWriter<DataSchemaNode> {

    public StructuredDataWriter(ConsoleIO console, String indent) {
        super(console, indent);
    }

    @Override
    public void writeInner(DataSchemaNode dataSchemaNode, List<Node<?>> dataNode) throws WriteException, IOException {

        if (dataNode.size() != 1) {
            return;
        }

        if (!(dataNode.get(0) instanceof CompositeNode)) {
            return;
        }

        Map<QName, List<Node<?>>> groupedNodes = groupNodes((CompositeNode) dataNode.get(0));

        if (dataSchemaNode instanceof DataNodeContainer) {
            writeChildren((DataNodeContainer) dataSchemaNode, groupedNodes);
        }
    }

    private Map<QName, List<Node<?>>> groupNodes(CompositeNode dataContainer) {
        Map<QName, List<Node<?>>> groupedNodes = new HashMap<>();
        for (Node<?> childDataNode : dataContainer.getValue()) {
            QName childQName = childDataNode.getNodeType();
            List<Node<?>> equalQNameChilds = groupedNodes.get(childQName);
            if (equalQNameChilds == null) {
                equalQNameChilds = new ArrayList<>();
                groupedNodes.put(childQName, equalQNameChilds);
            }
            equalQNameChilds.add(childDataNode);
        }
        return groupedNodes;

    }

    private void writeChildren(DataNodeContainer dataSchemaNode, Map<QName, List<Node<?>>> grouppedNodes)
            throws WriteException, IOException {

        List<Node<?>> choiceDataNodeCandidates = new ArrayList<>();
        for (QName qNameOfOutputNode : grouppedNodes.keySet()) {
            DataSchemaNode schemaNodeForDataNode = findSchemaNodeByQName(qNameOfOutputNode, dataSchemaNode);
            if (schemaNodeForDataNode == null) {
                choiceDataNodeCandidates.addAll(grouppedNodes.get(qNameOfOutputNode));
            } else {
                new GenericWriter(console, indent + indent()).write(schemaNodeForDataNode,
                        grouppedNodes.get(qNameOfOutputNode));
            }
        }
        if (!choiceDataNodeCandidates.isEmpty()) {
            for (Node<?> unwrittenNode : choiceDataNodeCandidates) {
                DataSchemaNode foundSchema = findSchemaNodesInChoiceByName(dataSchemaNode, unwrittenNode.getNodeType());

                List<Node<?>> unwrittenNodes = new ArrayList<>();
                unwrittenNodes.add(unwrittenNode);
                new GenericWriter(console, indent + indent()).write(foundSchema, unwrittenNodes);

            }
        }
    }

    // TODO : what if nodes with equal name exists in various choices?
    // TODO : what if nodes are from various cases in the same choice?
    // TODO : what if nodes are from the same case and choice at first level but
    // from various cases of the same choice at second and more level
    private DataSchemaNode findSchemaNodesInChoiceByName(DataNodeContainer dataSchemaNode, QName searchedQName) {
        for (DataSchemaNode choiceCandidate : dataSchemaNode.getChildNodes()) {
            if (choiceCandidate instanceof ChoiceNode) {
                ChoiceNode choiceNode = (ChoiceNode) choiceCandidate;
                for (ChoiceCaseNode caseCandidate : choiceNode.getCases()) {
                    DataSchemaNode searchedSchema = caseCandidate.getDataChildByName(searchedQName);
                    if (searchedSchema == null) {
                        searchedSchema = findSchemaNodesInChoiceByName(caseCandidate, searchedQName);
                    }
                    if (searchedSchema != null) {
                        return searchedSchema;
                    }
                }
            }
        }
        return null;
    }

    private DataSchemaNode findSchemaNodeByQName(QName childQName, DataNodeContainer dataSchemaNode) {
        return dataSchemaNode.getDataChildByName(childQName);
    }

}
