/**
 * 
 */
package org.opendaylight.controller.yang.data.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.ModifyAction;
import org.opendaylight.controller.yang.data.api.MutableCompositeNode;
import org.opendaylight.controller.yang.data.api.MutableNode;
import org.opendaylight.controller.yang.data.api.MutableSimpleNode;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.NodeModificationBuilder;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaContext;

/**
 * @author michal.rehak
 *
 */
public class NodeModificationBuilderImpl implements NodeModificationBuilder {
    
    private SchemaContext context;
    
    private Set<MutableNode<?>> changeLog;
    private Map<Node<?>, Node<?>> originalToMutable;

    private MutableCompositeNode mutableRoot;

    /**
     * @param originalTreeRootNode 
     * @param context
     */
    public NodeModificationBuilderImpl(CompositeNode originalTreeRootNode, SchemaContext context) {
        this.context = context;
        originalToMutable = new HashMap<>();
        mutableRoot = NodeFactory.copyDeepNode(originalTreeRootNode, originalToMutable);
        changeLog = new HashSet<>();
    }

    /**
     * add given node to it's parent's list of children
     * @param newNode
     */
    private static void fixParentRelation(Node<?> newNode) {
        if (newNode.getParent() != null) {
            List<Node<?>> siblings = newNode.getParent().getChildren();
            if (!siblings.contains(newNode)) {
                siblings.add(newNode);
            }
        }
    }

    /**
     * @param modNode
     * @param action 
     */
    private void addModificationToLog(MutableNode<?> modNode, ModifyAction action) {
        modNode.setModifyAction(action);
        changeLog.add(modNode);
    }

    @Override
    public void addNode(MutableSimpleNode<?> newNode) {
        fixParentRelation(newNode);
        addModificationToLog(newNode, ModifyAction.CREATE);
    }
    
    @Override
    public void addNode(MutableCompositeNode newNode) {
        fixParentRelation(newNode);
        addModificationToLog(newNode, ModifyAction.CREATE);
    }
    
    @Override
    public void replaceNode(MutableSimpleNode<?> replacementNode) {
        addModificationToLog(replacementNode, ModifyAction.REPLACE);
    }
    
    @Override
    public void replaceNode(MutableCompositeNode replacementNode) {
        addModificationToLog(replacementNode, ModifyAction.REPLACE);
    }

    @Override
    public void deleteNode(MutableCompositeNode deadNode) {
        addModificationToLog(deadNode, ModifyAction.DELETE);
    }
    
    @Override
    public void deleteNode(MutableSimpleNode<?> deadNode) {
        addModificationToLog(deadNode, ModifyAction.DELETE);
    }

    @Override
    public void removeNode(MutableSimpleNode<?> deadNode) {
        addModificationToLog(deadNode, ModifyAction.REMOVE);
    }
    
    @Override
    public void removeNode(MutableCompositeNode deadNode) {
        addModificationToLog(deadNode, ModifyAction.REMOVE);
    }
    
    @Override
    public void mergeNode(MutableCompositeNode alteredNode) {
        addModificationToLog(alteredNode, ModifyAction.MERGE);
    }

    /**
     * @return minimalistic tree containing diffs only
     */
    @Override
    public CompositeNode buildDiffTree() {
        Set<Node<?>> wanted = new HashSet<>();
        
        // walk changeLog, collect all required nodes
        for (MutableNode<?> mutant : changeLog) {
            wanted.addAll(collectSelfAndAllParents(mutant));
        }
        
        // TODO:: walk wanted and add relevant keys
        Map<String, ListSchemaNode>  mapOfLists = NodeUtils.buildMapOfListNodes(context);
        Set<Node<?>> wantedKeys = new HashSet<>();
        for (Node<?> outlaw : wanted) {
            if (outlaw instanceof CompositeNode) {
                String path = NodeUtils.buildPath(outlaw);
                if (mapOfLists.containsKey(path)) {
                    ListSchemaNode listSchema = mapOfLists.get(path);
                    if (listSchema.getQName().equals(outlaw.getNodeType())) {
                        // try to add key subnode to wanted list
                        List<QName> supportedKeys = listSchema.getKeyDefinition();
                        for (Node<?> outlawChildren : ((CompositeNode) outlaw).getChildren()) {
                            if (supportedKeys.contains(outlawChildren.getNodeType())) {
                                wantedKeys.add(outlawChildren);
                            }
                        }
                    }
                }
            }
        }
        wanted.addAll(wantedKeys);
        
        // remove all unwanted nodes from tree
        removeUnrelevantNodes(mutableRoot, wanted);
        
        return mutableRoot;
    }

    /**
     * @param mutableRoot2
     * @param wanted
     */
    private static void removeUnrelevantNodes(MutableCompositeNode mutRoot,
            Set<Node<?>> wanted) {
        Stack<MutableNode<?>> jobQueue = new Stack<>();
        jobQueue.push(mutRoot);
        while (!jobQueue.isEmpty()) {
            MutableNode<?> mutNode = jobQueue.pop();
            if (!wanted.contains(mutNode)) {
                if (mutNode.getParent() != null) {
                    mutNode.getParent().getChildren().remove(mutNode);
                }
            } else {
                if (mutNode instanceof MutableCompositeNode) {
                    for (Node<?> mutChild : ((MutableCompositeNode) mutNode).getChildren()) {
                        jobQueue.push((MutableNode<?>) mutChild);
                    }
                }
            }
        }
    }

    /**
     * @param focusedAncestor
     * @return set of parents and focusedAncestor itself
     */
    private static Set<Node<?>> collectSelfAndAllParents(Node<?> focusedAncestor) {
        Set<Node<?>> family = new HashSet<>();
        Node<?> tmpNode = focusedAncestor;
        while (tmpNode != null) {
            family.add(tmpNode);
            tmpNode = tmpNode.getParent();
        }
        return family;
    }

    /**
     * @param originalNode
     * @return mutable version of given node
     */
    @Override
    public Node<?> getMutableEquivalent(Node<?> originalNode) {
        return originalToMutable.get(originalNode);
    }

}
