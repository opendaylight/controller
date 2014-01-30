/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util;


import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.ConstraintDefinition;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UsesNode;

import static com.google.common.base.Preconditions.*;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

public class YangSchemaUtils {

    private static final Function<PathArgument, QName> QNAME_FROM_PATH_ARGUMENT = new Function<PathArgument, QName>(){
        
        @Override
        public QName apply(PathArgument input) {
            if(input == null) {
                return null;
            }
            return input.getNodeType();
        }
    };

    private  YangSchemaUtils() {
        throw new UnsupportedOperationException("Utility class.");
    }
    
    
    public static DataSchemaNode getSchemaNode(SchemaContext schema,InstanceIdentifier path) {
        checkArgument(schema != null,"YANG Schema must not be null.");
        checkArgument(path != null,"Path must not be null.");
        return getSchemaNode(schema, FluentIterable.from(path.getPath()).transform(QNAME_FROM_PATH_ARGUMENT));
    }
    
    public static DataSchemaNode getSchemaNode(SchemaContext schema,Iterable<QName> path) {
        checkArgument(schema != null,"YANG Schema must not be null.");
        checkArgument(path != null,"Path must not be null.");
        if(!path.iterator().hasNext()){
            return toRootDataNode(schema);
        }
        
        QName firstNode = path.iterator().next();
        DataNodeContainer previous = schema.findModuleByNamespaceAndRevision(firstNode.getNamespace(),
                firstNode.getRevision());
        Iterator<QName> iterator = path.iterator();
        
        while (iterator.hasNext()) {
            checkArgument(previous!= null, "Supplied path does not resolve into valid schema node.");
            QName arg = iterator.next();
            DataSchemaNode currentNode = previous.getDataChildByName(arg);
            if (currentNode == null && previous instanceof DataNodeContainer) {
                currentNode = searchInChoices(previous, arg);
            }
            if (currentNode instanceof DataNodeContainer) {
                previous = (DataNodeContainer) currentNode;
            } else if (currentNode instanceof LeafSchemaNode || currentNode instanceof LeafListSchemaNode) {
                checkArgument(!iterator.hasNext(), "Path nests inside leaf node, which is not allowed.");
                return currentNode;
            }
            checkState(currentNode != null, "Current node should not be null for %s",path);
        }
        checkState(previous instanceof DataSchemaNode, "Schema node for %s should be instance of DataSchemaNode. Found %s",path,previous);
        return (DataSchemaNode) previous;
    }

    private static DataSchemaNode searchInChoices(DataNodeContainer node, QName arg) {
        Set<DataSchemaNode> children = node.getChildNodes();
        for (DataSchemaNode child : children) {
            if (child instanceof ChoiceNode) {
                ChoiceNode choiceNode = (ChoiceNode) child;
                DataSchemaNode potential = searchInCases(choiceNode, arg);
                if (potential != null) {
                    return potential;
                }
            }
        }
        return null;
    }

    private static DataSchemaNode searchInCases(ChoiceNode choiceNode, QName arg) {
        Set<ChoiceCaseNode> cases = choiceNode.getCases();
        for (ChoiceCaseNode caseNode : cases) {
            DataSchemaNode node = caseNode.getDataChildByName(arg);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    private static ContainerSchemaNode toRootDataNode(SchemaContext schema) {
        return new NetconfDataRootNode(schema);
    }

    private static final class NetconfDataRootNode implements ContainerSchemaNode {
    
        public NetconfDataRootNode(SchemaContext schema) {
            // TODO Auto-generated constructor stub
        }
    
        @Override
        public Set<TypeDefinition<?>> getTypeDefinitions() {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public Set<DataSchemaNode> getChildNodes() {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public Set<GroupingDefinition> getGroupings() {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public DataSchemaNode getDataChildByName(QName name) {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public DataSchemaNode getDataChildByName(String name) {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public Set<UsesNode> getUses() {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public Set<AugmentationSchema> getAvailableAugmentations() {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public boolean isAugmenting() {
            // TODO Auto-generated method stub
            return false;
        }
    
        @Override
        public boolean isAddedByUses() {
            // TODO Auto-generated method stub
            return false;
        }
    
        @Override
        public boolean isConfiguration() {
            // TODO Auto-generated method stub
            return false;
        }
    
        @Override
        public ConstraintDefinition getConstraints() {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public QName getQName() {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public SchemaPath getPath() {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public String getDescription() {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public String getReference() {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public Status getStatus() {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public boolean isPresenceContainer() {
            // TODO Auto-generated method stub
            return false;
        }
    
    }

}
