/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.gson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

class CompositeNodeDataWithSchema extends NodeDataWithSchema {

    /**
     * nodes which were added to schema via augmentation and are present in data input
     */
    protected Map<AugmentationSchema, List<NodeDataWithSchema>> augmentationsToChild = new HashMap<>();

    /**
     * remaining data nodes (which aren't added via augment). Every of them should have the same QName
     */
    protected List<NodeDataWithSchema> childs = new ArrayList<>();

    public CompositeNodeDataWithSchema(final DataSchemaNode schema) {
        super(schema);
    }

    public NodeDataWithSchema addSimpleChild(final DataSchemaNode schema) {
        SimpleNodeDataWithSchema newChild = null;
        if (schema instanceof LeafSchemaNode) {
            newChild = new LeafNodeDataWithSchema(schema);
        } else if (schema instanceof AnyXmlSchemaNode) {
            newChild = new AnyXmlNodeDataWithSchema(schema);
        }

        if (newChild != null) {

            AugmentationSchema augSchema = null;
            if (schema.isAugmenting()) {
                augSchema = findCorrespondingAugment(this.schema, schema);
            }
            if (augSchema != null) {
                addChildToAugmentation(augSchema, newChild);
            } else {
                addChild(newChild);
            }
            return newChild;
        }
        return null;
    }

    private void addChildToAugmentation(AugmentationSchema augSchema, NodeDataWithSchema newChild) {
        List<NodeDataWithSchema> childsInAugment = augmentationsToChild.get(augSchema);
        if (childsInAugment == null) {
            childsInAugment = new ArrayList<>();
            augmentationsToChild.put(augSchema, childsInAugment);
        }
        childsInAugment.add(newChild);
    }

    public NodeDataWithSchema addChild(final Stack<DataSchemaNode> schemas) {
        if (schemas.size() == 1) {
            final DataSchemaNode childDataSchemaNode = schemas.pop();
            return addChild(childDataSchemaNode);
        } else {
            DataSchemaNode choiceCandidate = schemas.pop();
            DataSchemaNode caseCandidate = schemas.pop();
            ChoiceNode choiceNode = null;
            ChoiceCaseNode caseNode = null;
            if (choiceCandidate instanceof ChoiceNode) {
                choiceNode = (ChoiceNode) choiceCandidate;
            } else {
                throw new IllegalArgumentException("Awaited node of type ChoiceNode but was "
                        + choiceCandidate.getClass().getSimpleName());
            }

            if (caseCandidate instanceof ChoiceCaseNode) {
                caseNode = (ChoiceCaseNode) caseCandidate;
            } else {
                throw new IllegalArgumentException("Awaited node of type ChoiceCaseNode but was "
                        + caseCandidate.getClass().getSimpleName());
            }

            AugmentationSchema augSchema = null;
            if (choiceCandidate.isAugmenting()) {
                augSchema = findCorrespondingAugment(this.schema, choiceCandidate);
            }

            // looking for existing choice
            List<NodeDataWithSchema> childNodes = Collections.emptyList();
            if (augSchema != null) {
                childNodes = augmentationsToChild.get(augSchema);
            } else {
                childNodes = childs;
            }

            CompositeNodeDataWithSchema caseNodeDataWithSchema = findChoice(childNodes, choiceCandidate, caseCandidate);
            if (caseNodeDataWithSchema == null) {
                ChoiceNodeDataWithSchema choiceNodeDataWithSchema = new ChoiceNodeDataWithSchema(choiceNode);
                addChild(choiceNodeDataWithSchema);
                caseNodeDataWithSchema = choiceNodeDataWithSchema.addCompositeChild(caseNode);
            }

            return caseNodeDataWithSchema.addChild(schemas);
        }

    }

    private CaseNodeDataWithSchema findChoice(List<NodeDataWithSchema> childNodes, DataSchemaNode choiceCandidate,
            DataSchemaNode caseCandidate) {
        if (childNodes == null) {
            return null;
        }
        for (NodeDataWithSchema nodeDataWithSchema : childNodes) {
            if (nodeDataWithSchema instanceof ChoiceNodeDataWithSchema
                    && nodeDataWithSchema.getSchema().getQName().equals(choiceCandidate.getQName())) {
                CaseNodeDataWithSchema casePrevious = ((ChoiceNodeDataWithSchema) nodeDataWithSchema).getCase();
                if (casePrevious.getSchema().getQName() != caseCandidate.getQName()) {
                    throw new IllegalArgumentException("Data from case " + caseCandidate.getQName()
                            + " are specified but other data from case " + casePrevious.getSchema().getQName()
                            + " were specified erlier. Data aren't from the same case.");
                }
                return casePrevious;
            }
        }
        return null;
    }

    public NodeDataWithSchema addCompositeChild(final DataSchemaNode schema) {
        CompositeNodeDataWithSchema newChild;
        if (schema instanceof ListSchemaNode) {
            newChild = new ListNodeDataWithSchema(schema);
        } else if (schema instanceof LeafListSchemaNode) {
            newChild = new LeafListNodeDataWithSchema(schema);
        } else if (schema instanceof ContainerSchemaNode) {
            newChild = new ContainerNodeDataWithSchema(schema);
        } else {
            newChild = new CompositeNodeDataWithSchema(schema);
        }
        addCompositeChild(newChild);
        return newChild;
    }

    public void addCompositeChild(final CompositeNodeDataWithSchema newChild) {
        AugmentationSchema augSchema = findCorrespondingAugment(this.schema, newChild.getSchema());
        if (augSchema != null) {
            addChildToAugmentation(augSchema, newChild);
        } else {
            addChild(newChild);
        }
    }

    private NodeDataWithSchema addChild(final DataSchemaNode schema) {
        NodeDataWithSchema newChild = addSimpleChild(schema);
        return newChild == null ? addCompositeChild(schema) : newChild;
    }

    public void addChild(final NodeDataWithSchema newChild) {
        childs.add(newChild);
    }

    /**
     * Tries to find in {@code parent} which is dealed as augmentation target node with QName as {@code child}. If such
     * node is found then it is returned, else null.
     */
    protected AugmentationSchema findCorrespondingAugment(DataSchemaNode parent, DataSchemaNode child) {
        if (parent instanceof AugmentationTarget) {
            for (AugmentationSchema augmentation : ((AugmentationTarget) parent).getAvailableAugmentations()) {
                DataSchemaNode childInAugmentation = augmentation.getDataChildByName(child.getQName());
                if (childInAugmentation != null) {
                    return augmentation;
                }
            }
        }
        return null;
    }

    @Override
    public void writeToStream(final NormalizedNodeStreamWriter nnStreamWriter) {
        for (NodeDataWithSchema child : childs) {
            child.writeToStream(nnStreamWriter);
        }
        for (Entry<AugmentationSchema, List<NodeDataWithSchema>> augmentationToChild : augmentationsToChild.entrySet()) {

            final List<NodeDataWithSchema> childsFromAgumentation = augmentationToChild.getValue();

            if (!childsFromAgumentation.isEmpty()) {
                nnStreamWriter.startAugmentationNode(toAugmentationIdentifier(augmentationToChild));

                for (NodeDataWithSchema nodeDataWithSchema : childsFromAgumentation) {
                    nodeDataWithSchema.writeToStream(nnStreamWriter);
                }

                nnStreamWriter.endNode();
            }
        }
    }

    private AugmentationIdentifier toAugmentationIdentifier(
            final Entry<AugmentationSchema, List<NodeDataWithSchema>> augmentationToChild) {
        Collection<DataSchemaNode> nodes = augmentationToChild.getKey().getChildNodes();
        Set<QName> nodesQNames = new HashSet<>();
        for (DataSchemaNode node : nodes) {
            nodesQNames.add(node.getQName());
        }

        return new AugmentationIdentifier(nodesQNames);
    }

}
