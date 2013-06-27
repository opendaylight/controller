/**
 * 
 */
package org.opendaylight.controller.yang.data.api;


/**
 * @author michal.rehak
 *
 */
public interface NodeModificationBuilder {

    public abstract Node<?> getMutableEquivalent(Node<?> originalNode);

    public abstract CompositeNode buildDiffTree();

    public abstract void mergeNode(MutableCompositeNode alteredNode);

    public abstract void removeNode(MutableCompositeNode deadNode);

    public abstract void removeNode(MutableSimpleNode<?> deadNode);

    public abstract void deleteNode(MutableSimpleNode<?> deadNode);

    public abstract void deleteNode(MutableCompositeNode deadNode);

    public abstract void replaceNode(MutableCompositeNode replacementNode);

    public abstract void replaceNode(MutableSimpleNode<?> replacementNode);

    public abstract void addNode(MutableCompositeNode newNode);

    public abstract void addNode(MutableSimpleNode<?> newNode);

}
