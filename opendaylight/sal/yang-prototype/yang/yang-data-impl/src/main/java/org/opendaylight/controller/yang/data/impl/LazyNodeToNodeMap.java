/**
 * 
 */
package org.opendaylight.controller.yang.data.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.MutableCompositeNode;
import org.opendaylight.controller.yang.data.api.MutableNode;
import org.opendaylight.controller.yang.data.api.MutableSimpleNode;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.NodeModification;
import org.opendaylight.controller.yang.data.api.SimpleNode;

/**
 * @author michal.rehak
 *
 */
public class LazyNodeToNodeMap {
    
    private Map<Node<?>, Node<?>> node2node = new HashMap<>();
    private CompositeNode originalRoot;
    private MutableCompositeNode mutableRoot;
    
    /**
     * @param originalNode
     * @return mutable twin
     */
    public Node<?> getMutableEquivalent(Node<?> originalNode) {
        Node<?> mutableNode = node2node.get(originalNode);
        if (mutableNode == null) {
            addPathMembers(originalNode);
            mutableNode = node2node.get(originalNode);
        }
        
        return mutableNode;
    }
    
    /**
     * @param originalNode
     */
    private void addPathMembers(Node<?> originalNode) {
        Stack<Node<?>> jobQueue = new Stack<>(); 
        jobQueue.push(originalNode);
        while (!jobQueue.isEmpty()) {
            Node<?> node2add = jobQueue.pop();
            boolean fixChildrenRefOnly = false;
            if (node2node.containsKey(node2add)) {
                if (node2add instanceof SimpleNode<?>) {
                    continue;
                }
                fixChildrenRefOnly = true;
            }
            
            CompositeNode nextParent = node2add.getParent();
            MutableNode<?> mutableEquivalent = null;
            
            if (node2add instanceof SimpleNode<?>) {
                SimpleNode<?> node2addSimple = (SimpleNode<?>) node2add;
                MutableSimpleNode<?> nodeMutant = NodeFactory.createMutableSimpleNode(
                        node2add.getNodeType(), null, node2addSimple.getValue(), 
                        node2addSimple.getModificationAction(), node2addSimple);
                mutableEquivalent = nodeMutant;
            } else if (node2add instanceof CompositeNode) {
                MutableCompositeNode nodeMutant = null;
                if (fixChildrenRefOnly) {
                    nodeMutant = (MutableCompositeNode) node2node.get(node2add);
                } else {
                    CompositeNode node2addComposite = (CompositeNode) node2add;
                    nodeMutant = NodeFactory.createMutableCompositeNode(node2add.getNodeType(), 
                        null, null, 
                        ((NodeModification) node2add).getModificationAction(), node2addComposite);
                }
                
                mutableEquivalent = nodeMutant;

                // tidy up children
                if (nodeMutant.getChildren() == null) {
                    nodeMutant.setValue(new ArrayList<Node<?>>());
                }
                for (Node<?> originalChildNode : ((CompositeNode) node2add).getChildren()) {
                    MutableNode<?> mutableChild = (MutableNode<?>) node2node.get(originalChildNode);
                    fixChildrenRef(nodeMutant, mutableChild);
                }
                
                if (nodeMutant.getChildren() != null && !nodeMutant.getChildren().isEmpty()) {
                    nodeMutant.init();
                }

                // store tree root, if occured
                if (nextParent == null) {
                    if (originalRoot == null) {
                        originalRoot = (CompositeNode) node2add;
                        mutableRoot = nodeMutant;
                    } else {
                        if (!originalRoot.equals(node2add)) {
                            throw new IllegalStateException("Different tree root node obtained - " +
                            		"perhaps nodes of different trees are getting mixed up.");
                        }
                    }
                }
            }
            
            // feed jobQueue
            node2node.put(node2add, mutableEquivalent);
            if (nextParent != null) {
                jobQueue.push(nextParent);
            } 
        }
    }

    /**
     * @param nodeMutant
     * @param mutableChild
     */
    private static void fixChildrenRef(MutableCompositeNode nodeMutant,
            MutableNode<?> mutableChild) {
        if (mutableChild != null) {
            if (!nodeMutant.getChildren().contains(mutableChild)) {
                nodeMutant.getChildren().add(mutableChild);
            }
            CompositeNode parentOfChild = mutableChild.getParent();
            if (parentOfChild == null) {
                mutableChild.setParent(nodeMutant);
            } else {
                if (!parentOfChild.equals(nodeMutant)) {
                    throw new IllegalStateException("Different parent node obtained - " +
                            "perhaps nodes of different trees are getting mixed up.");
                }
            }
        }
    }

    /**
     * @return the mutableRoot
     */
    public MutableCompositeNode getMutableRoot() {
        return mutableRoot;
    }
    
    /**
     * @return set of original nodes, registered in map as keys 
     */
    public Set<Node<?>> getKeyNodes() {
        return node2node.keySet();
    }
}
