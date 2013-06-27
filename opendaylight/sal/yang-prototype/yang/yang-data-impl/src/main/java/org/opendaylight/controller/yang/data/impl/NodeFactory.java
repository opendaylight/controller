/**
 * 
 */
package org.opendaylight.controller.yang.data.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.ModifyAction;
import org.opendaylight.controller.yang.data.api.MutableCompositeNode;
import org.opendaylight.controller.yang.data.api.MutableSimpleNode;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.SimpleNode;

/**
 * @author michal.rehak
 * 
 */
public abstract class NodeFactory {

    /**
     * @param qName
     * @param parent
     * @param value
     * @return simple node modification, based on given qname, value and parent
     */
    public static <T> SimpleNode<T> createSimpleNode(QName qName,
            CompositeNode parent, T value) {
        SimpleNodeTOImpl<T> simpleNodeTOImpl = new SimpleNodeTOImpl<T>(qName, parent, value);
        return simpleNodeTOImpl;
    }
    
    /**
     * @param qName
     * @param parent
     * @param value
     * @return simple node modification, based on given qname, value and parent
     */
    public static <T> MutableSimpleNode<T> createMutableSimpleNode(QName qName,
            CompositeNode parent, T value) {
        MutableSimpleNodeTOImpl<T> simpleNodeTOImpl = 
                new MutableSimpleNodeTOImpl<T>(qName, parent, value, null);
        return simpleNodeTOImpl;
    }

    /**
     * @param qName
     * @param parent
     * @param value
     * @return composite node modification, based on given qname, value (children), parent and modifyAction
     */
    public static CompositeNode createCompositeNode(QName qName,
            CompositeNode parent, List<Node<?>> value) {
        CompositeNode compositeNodeTOImpl = new CompositeNodeTOImpl(qName, parent, value);
        return compositeNodeTOImpl;
    }
    
    /**
     * @param qName
     * @param parent
     * @param value
     * @return composite node modification, based on given qname, value (children), parent and modifyAction
     */
    public static MutableCompositeNode createMutableCompositeNode(QName qName,
            CompositeNode parent, List<Node<?>> value) {
        MutableCompositeNodeTOImpl compositeNodeTOImpl = 
                new MutableCompositeNodeTOImpl(qName, parent, value, null);
        return compositeNodeTOImpl;
    }
    
    
    /**
     * @param qName
     * @param parent
     * @param value
     * @param modifyAction
     * @return simple node modification, based on given qname, value, parent and modifyAction
     */
    public static <T> SimpleNodeModificationTOImpl<T> createSimpleNodeModification(QName qName,
            CompositeNode parent, T value, ModifyAction modifyAction) {
        SimpleNodeModificationTOImpl<T> simpleNodeModTOImpl = 
                new SimpleNodeModificationTOImpl<T>(qName, parent, value, modifyAction);
        return simpleNodeModTOImpl;
    }

    /**
     * @param qName
     * @param parent
     * @param value
     * @param modifyAction 
     * @return composite node modification, based on given qname, value (children), parent and modifyAction
     */
    public static CompositeNodeModificationTOImpl createCompositeNodeModification(QName qName,
            CompositeNode parent, List<Node<?>> value, ModifyAction modifyAction) {
        CompositeNodeModificationTOImpl compositeNodeModTOImpl = 
                new CompositeNodeModificationTOImpl(qName, parent, value, modifyAction);
        return compositeNodeModTOImpl;
    }

    /**
     * @param node
     * @return copy of given node, parent and value are the same, but parent 
     * has no reference to this copy 
     */
    public static <T> SimpleNode<T> copyNode(SimpleNode<T> node) {
        SimpleNode<T> twinNode = createSimpleNode(
                    node.getNodeType(), node.getParent(), node.getValue());
        return twinNode;
    }
    
    /**
     * @param node
     * @return copy of given node, parent and value are the same, but parent 
     * has no reference to this copy 
     */
    public static <T> SimpleNode<T> copyNodeAsMutable(SimpleNode<T> node) {
        SimpleNode<T> twinNode = createMutableSimpleNode(
                    node.getNodeType(), node.getParent(), node.getValue());
        return twinNode;
    }

    /**
     * @param node
     * @param children 
     * @return copy of given node, parent and children are the same, but parent and children 
     * have no reference to this copy
     */
    public static CompositeNode copyNode(CompositeNode node, Node<?>... children) {
        CompositeNode twinNode = createCompositeNode(
                node.getNodeType(), node.getParent(), Arrays.asList(children));
        return twinNode;
    }
    
    /**
     * @param node
     * @return copy of given node, parent and children are the same, but parent and children 
     * have no reference to this copy
     */
    public static CompositeNode copyNode(CompositeNode node) {
       return copyNode(node, node.getChildren().toArray(new Node<?>[0]));
    }
    
    /**
     * @param node root of original tree
     * @param originalToMutable (optional) empty map, where binding between original and copy 
     * will be stored
     * @return copy of given node, parent and children are the same, but parent and children 
     * have no reference to this copy
     */
    public static MutableCompositeNode copyDeepNode(CompositeNode node, 
            Map<Node<?>, Node<?>> originalToMutable) {
              
       MutableCompositeNode mutableRoot = 
               createMutableCompositeNode(node.getNodeType(), null, null);
       Stack<SimpleEntry<CompositeNode, MutableCompositeNode>> jobQueue = new Stack<>();
       jobQueue.push(new SimpleEntry<CompositeNode, MutableCompositeNode>(node, mutableRoot));
       if (originalToMutable != null) {
           originalToMutable.put(node, mutableRoot);
       }
       
       while (!jobQueue.isEmpty()) {
           SimpleEntry<CompositeNode, MutableCompositeNode> job = jobQueue.pop();
           CompositeNode originalNode = job.getKey();
           MutableCompositeNode mutableNode = job.getValue();
           mutableNode.setValue(new ArrayList<Node<?>>());
           
           for (Node<?> child : originalNode.getChildren()) {
               Node<?> mutableAscendant = null;
               if (child instanceof CompositeNode) {
                   MutableCompositeNode newMutable = 
                           createMutableCompositeNode(child.getNodeType(), mutableNode, null);
                   jobQueue.push(new SimpleEntry<CompositeNode, MutableCompositeNode>(
                           (CompositeNode) child, newMutable));
                   mutableAscendant = newMutable;
               } else if (child instanceof SimpleNode<?>) {
                   mutableAscendant = 
                           createMutableSimpleNode(child.getNodeType(), mutableNode, child.getValue());
               } else {
                   throw new IllegalStateException("Node class deep copy not supported: "
                           +child.getClass().getName());
               }
               
               mutableNode.getChildren().add(mutableAscendant);
               if (originalToMutable != null) {
                   originalToMutable.put(child, mutableAscendant);
               }
           }
           mutableNode.init();
       }
       
       return mutableRoot;
    }
    
}
