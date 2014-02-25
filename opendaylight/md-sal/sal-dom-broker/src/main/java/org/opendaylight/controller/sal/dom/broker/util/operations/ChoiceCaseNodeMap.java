/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util.operations;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import org.opendaylight.controller.sal.dom.broker.util.YangSchemaUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

import java.util.*;

final class ChoiceCaseNodeMap {

    public static final int CHOICE_QNAME_POSITION = 1;
    public static final int EXPECTED_PATH_SIZE = 4;
    public static final int CASE_QNAME_POSITION = 2;
    private final Map<QName, ChoiceMap> choiceMaps;

    private ChoiceCaseNodeMap(Map<QName, ChoiceMap> choiceMaps) {
        this.choiceMaps = choiceMaps;
    }

    Map<QName, ChoiceMap> getMappedChoices() {
        return choiceMaps;
    }

    static ChoiceCaseNodeMap fromUnknownNodes(DataNodeContainer schema, List<QName> unknownChildNodes) throws DataModificationException {
        if(unknownChildNodes.isEmpty()) {
            return new ChoiceCaseNodeMap(Collections.<QName, ChoiceMap>emptyMap());
        }

        Map<QName, ChoiceMap> choiceMaps = Maps.newHashMap();

        for (QName unknownChildNode : unknownChildNodes) {
            DataSchemaNode inChoiceChild = YangSchemaUtils.searchInChoices(schema, unknownChildNode);

            if(inChoiceChild == null) {
                // This child is unknown and does not belong to any choice/case
                // Throw ex with unknown node
                DataModificationException.UnknownNodeException.throwUnknownNode(unknownChildNode, schema);
            }

            List<QName> childPath = inChoiceChild.getPath().getPath();
            Preconditions.checkArgument(childPath.size()== EXPECTED_PATH_SIZE);
            QName choiceQName = childPath.get(CHOICE_QNAME_POSITION);
            Preconditions.checkState(choiceQName != null);

            ChoiceMap choiceMap = choiceMaps.get(choiceQName);
            if(choiceMap==null) {
                choiceMap = new ChoiceMap(choiceQName);
                choiceMaps.put(choiceQName, choiceMap);
            }

            choiceMap.add(inChoiceChild);
        }
        return new ChoiceCaseNodeMap(choiceMaps);
    }

    static final class ChoiceMap {
        private final QName choiceQName;

        private final Multimap<QName, QName> caseToNodes;
        private final Map<QName, QName> nodeToCase;
        private final Map<QName, DataSchemaNode> nodeToSchema;

        ChoiceMap(QName choiceQName) {
            this.choiceQName = choiceQName;
            caseToNodes = HashMultimap.create();
            nodeToCase = Maps.newHashMap();
            nodeToSchema = Maps.newHashMap();
        }

        private void add(DataSchemaNode inChoiceChild) throws DataModificationException.IllegalChoiceValuesException {
            List<QName> childPath = inChoiceChild.getPath().getPath();
            QName caseQName = childPath.get(CASE_QNAME_POSITION);
            QName nodeQName = childPath.get(CASE_QNAME_POSITION + 1);

            if(nodeToCase.containsKey(nodeQName)) {
                DataModificationException.IllegalChoiceValuesException.throwDuplicateChild(choiceQName, caseQName, nodeQName);
            }
            nodeToCase.put(nodeQName, caseQName);
            nodeToSchema.put(nodeQName, inChoiceChild);
            caseToNodes.put(caseQName, nodeQName);
        }

        Collection<QName> getAllNodes() {
            return nodeToCase.keySet();
        }

        public QName getCaseQNameForNodeQName(QName nodeFromCase) {
            Preconditions.checkArgument(nodeToCase.containsKey(nodeFromCase), "Node %s is not a child of %s", nodeFromCase, choiceQName);
            return nodeToCase.get(nodeFromCase);
        }

        public Collection<QName> getChildNodesForCaseQName(QName caseQName) {
            Preconditions.checkArgument(caseToNodes.containsKey(caseQName), "Case %s not present in %s", caseQName, choiceQName);
            return caseToNodes.get(caseQName);
        }

        public DataSchemaNode getSchemaForNode(QName nodeQName) {
            Preconditions.checkArgument(nodeToSchema.containsKey(nodeQName), "Node %s, unknown in choice", nodeQName, choiceQName);
            return nodeToSchema.get(nodeQName);
        }
    }
}
