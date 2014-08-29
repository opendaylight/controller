/*
 * Author : Neel Bommisetty
 * Email : neel250294@gmail.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.xpath;

import java.util.Iterator;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.google.common.collect.Iterators;

/**
 * Defines an element node which proxies an MD-SAL NormalizedNode allowing for xpath querying
 * on-the-fly.
 *
 * @author Devin Avery
 *
 */
public class NormalizedNodeElement extends ThrowExceptionElement implements XPathNodeProxy {

    private static final Logger logger = LoggerFactory.getLogger(NormalizedNodeElement.class);

    private boolean isFirstInit = false;
    private Node firstChild;

    private boolean isFirstSiblingInit = false;
    private NormalizedNodeElement nextSibling;
    private final Iterator<NormalizedNode<?,?>> nextSiblingItr;

    public NormalizedNodeElement(NormalizedNode<?, ?> node1, Node parent1) {
        this( node1, parent1, null );
    }

    NormalizedNodeElement(NormalizedNode<?, ?> nodeDelegate, Node parentNode,
            Iterator<NormalizedNode<?,?>> nextSib) {
        super( nodeDelegate, parentNode );
        nextSiblingItr = nextSib;

    }

    @Override
    public short getNodeType() {
        return Node.ELEMENT_NODE;

    }

    @Override
    public String getNodeValue() {
        return null;
    }

    @Override
    public boolean hasChildNodes() {
        return getFirstChild() != null;
    }

    @Override
    public NamedNodeMap getAttributes() {
        return NamedNodeMapImpl.EMPTY_MAP;
    }

    //Some funny business happening here. See http://stackoverflow.com/questions/543049/default-xml-namespace-jdom-and-xpath
    //for some details on how xpath works with xml etc. Will need to figure this out when we
    //start looking at API for integrating.
    @Override
    public String getNamespaceURI() {
        return getProxiedNode().getNodeType().getNamespace().toString();
    }

    @Override
    public String getNodeName() {
        return getProxiedNode().getNodeType().getLocalName();
    }

    /**
     * Returns the next sibling of this node, calculating it on the fly.
     */
    @Override
    public Node getNextSibling() {
        if (!isFirstSiblingInit) {
            if (nextSiblingItr.hasNext()) {
                nextSibling = createNormalizedNodeElement(
                        nextSiblingItr.next(), getParentNode(), nextSiblingItr);
            }
            isFirstSiblingInit = true;

        }
        return nextSibling;
    }

    /**
     * Calculates the first child on the fly when queried.
     */
    @Override
    public Node getFirstChild() {
        if (!isFirstInit) {

            NormalizedNode<?,?> nodeToProxy = getProxiedNode();
//            System.out.println("Get First Child on " + getLocalName());
            if (nodeToProxy instanceof LeafNode ||
                nodeToProxy instanceof org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode) {

                //Leaf nodes and LeafSetEntryNodes get turned into Text.
                firstChild = new NormalizedNodeProxy(nodeToProxy, this );

            } else if (nodeToProxy instanceof LeafSetNode) {

                //We have to special case the LeafSetNode here because a leaf set
                //is not a DataContainerNode
                firstChild = createNormalizedNodeElement(nodeToProxy, this, null);
            }
            else if ( nodeToProxy instanceof DataContainerNode ){

                //This handles all of the ordered / unordered lists as well as the common parent
                //  / child container relationships.
                DataContainerNode<?> dataContainerNode = (DataContainerNode<?>) nodeToProxy;
                Iterator<DataContainerChild<? extends PathArgument, ?>> dataiterator =
                                                        dataContainerNode.getValue().iterator();
                DataContainerChild<? extends PathArgument, ?> firstChildContainer = dataiterator
                        .next();
                firstChild = createNormalizedNodeElement(firstChildContainer, this, dataiterator);
            } else {
                //haven't come across any else's yet, but if we do make sure we detect and fix
                //those!
                throw new SalXPathRuntimeException( "Unhandled type when processing first child" );
            }

            isFirstInit = true;
        }
        return firstChild;
    }

    /**
     * Creates a NormalizedNodeElement (xpath compatible) for the given normalized node, unwrapping
     * and list or map that needs to outter wrapper removed.
     * @param node
     * @param parent
     * @param nextSiblingForNodeIterator
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private NormalizedNodeElement createNormalizedNodeElement(
            NormalizedNode<? extends PathArgument, ?> node,
            Node parent,
            Iterator nextSiblingForNodeIterator) {

        NormalizedNodeElement elem = null;

        // In this block of code we need to remove the logical list or map
        // object that wraps the children
        // since in the output XML this node is ignored.
        if (node instanceof UnkeyedListNode ||
            node instanceof LeafSetNode ||
            node instanceof MapNode) {

            DataContainerChild<?, Iterable> unkeyedListNode = (DataContainerChild<?, Iterable>) node;
            Iterable listChildren = unkeyedListNode.getValue();
            Iterator listChildItr = listChildren.iterator();
            Iterator concatinatedChildren = null;
            if (nextSiblingForNodeIterator == null) {
                concatinatedChildren = listChildItr;
            } else {
                concatinatedChildren = Iterators.concat(listChildItr, nextSiblingForNodeIterator);
            }

            elem = new NormalizedNodeElement((NormalizedNode<?, ?>) concatinatedChildren.next(),
                    parent, concatinatedChildren);
        } else {
            elem = new NormalizedNodeElement(node, parent, nextSiblingForNodeIterator);
        }

        return elem;
    }

    @Override
    public String getTextContent() {
        StringBuilder builder = new StringBuilder();
        if( this.hasChildNodes() ){
            Node child = getFirstChild();
            while( child != null ){
                builder.append( child.getTextContent() );
                child = child.getNextSibling();
            }
        }

        return builder.toString();
    }
}
