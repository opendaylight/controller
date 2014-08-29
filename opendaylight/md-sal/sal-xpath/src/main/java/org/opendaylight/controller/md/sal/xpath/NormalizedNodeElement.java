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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.google.common.collect.Iterators;

/*
 *
 */
public class NormalizedNodeElement extends ThrowExceptionElement {

    boolean hasChildren;
    NormalizedNode<?, ?> node;
    NormalizedNodeElement parent;

    NormalizedNodeElement lastChild;
    NormalizedNodeElement[] Children;

    boolean isFirstInit = false;
    Node firstChild;

    boolean isFirstSiblingInit = false;
    NormalizedNodeElement nextSibling;
    @SuppressWarnings("rawtypes")
    Iterator iterator;

    public NormalizedNodeElement(NormalizedNode<?, ?> node1,
            NormalizedNodeElement parent1) {
        node = node1;
        parent = parent1;

    }

    @SuppressWarnings("rawtypes")
    NormalizedNodeElement(NormalizedNode<?, ?> nodeDelegate,NormalizedNodeElement parentNode,Iterator nextSib) {
        // store nodeDelegate to class variable
        // store parent to class variable.
        // store the iterator to the next sibling
        node = nodeDelegate;
        parent = parentNode;
        iterator = nextSib;

    }

    @Override
    public short getNodeType() {
        return Node.ELEMENT_NODE;

    }

    @Override
    public String getNamespaceURI() {
        return null;

    }

    @Override
    public String getNodeValue() {
        return null;
    }

    @Override
    public boolean hasChildNodes() {
        return (node.getValue() instanceof Iterable);
    }

    @Override
    public boolean hasAttributes() {
        return false;
    }

    @Override
    public NamedNodeMap getAttributes() {
        return NamedNodeMapImpl.EMPTY_MAP;
    }

    @Override
    public String getNodeName() {

        return node.getNodeType().getLocalName();
    }

    @Override
    public Node getParentNode() {
        return parent;
    }

    @Override
    public String getLocalName() {
        return this.getNodeName();
    }

    @Override
    public Node getNextSibling() {
        if (!isFirstSiblingInit) {
            // if nextSib iterator is not null, then call next() on it to get
            // next sibling.
            // construct a NodeBuilderElement, passing in the new node, the same
            // parent, and the same iterator
            // cache the node builder element to first child

            if (iterator.hasNext()) {
                nextSibling = new NormalizedNodeElement((NormalizedNode<?, ?>) iterator.next(), this,this.iterator);
            }
            isFirstSiblingInit = true;

        }
        return nextSibling;
    }


    @SuppressWarnings("unchecked")
    @Override
    public Node getFirstChild() {
        if (!isFirstInit) {

            if(node instanceof org.opendaylight.yangtools.yang.data.api.schema.LeafNode||
                    node instanceof org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode)
            {
              //  System.out.println("The node is "+ this.getNodeName());
              //  System.out.println("The above node is either leaf node or leafsetentry node");
                firstChild = new NormalizedNodeText(node.getValue().toString(),this,null);
            }
            else if(node instanceof org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode )
            {
                // handle list here ..
               // System.out.println("The node is "+ this.getNodeName());


                Iterable<DataContainerChild<? extends PathArgument, ?>> list=((Iterable<DataContainerChild<? extends PathArgument, ?>>) node.getValue());

                Iterator<DataContainerChild<? extends PathArgument, ?>> listiter = list.iterator();

                    DataContainerNode<?> dataContainerNode = (DataContainerNode<?>) listiter.next();

                    Iterator<DataContainerChild<? extends PathArgument, ?>> Childiterator = dataContainerNode
                            .getValue().iterator();
                    firstChild = new NormalizedNodeElement((NormalizedNode<?, ?>) iterator.next(), this,
                            Childiterator);
                    this.iterator=Iterators.concat(listiter,this.iterator);
                  //  System.out.println("its first child is "+ firstChild.getNodeName());


            }
            else if( node instanceof org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode )
            {
                // handle list here ..
              //  System.out.println("The node is "+ this.getNodeName());
                Iterable<NormalizedNode<?,?>> list=((Iterable<NormalizedNode<?,?>>)node.getValue());

                Iterator<NormalizedNode<?,?>>listiter = list.iterator();

                    firstChild = new NormalizedNodeText(listiter.next().getValue().toString(),this, null);
                    //TypeConflict here
                     this.iterator=Iterators.concat(listiter,this.iterator);
                //     System.out.println("its first child is "+ firstChild.getNodeName());
            }

            // get iterator on children, get first child from iterator
            else {
              //  System.out.println("The node is "+ this.getNodeName());
                DataContainerNode<?> dataContainerNode = (DataContainerNode<?>) node;
                Iterator<DataContainerChild<? extends PathArgument, ?>> dataiterator = dataContainerNode
                        .getValue().iterator();
                DataContainerChild<? extends PathArgument, ?> something = dataiterator.next();
             /*   if (something instanceof org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode )
                {
                    Iterable<NormalizedNode<?, ?>> containerlist=(Iterable<NormalizedNode<?,?>>) something.getValue();
                    Iterator<NormalizedNode<?, ?>> containerListIterator = containerlist.iterator();

                    NormalizedNode<?, ?> someNode = containerListIterator.next();

                    firstChild = new NodeBuilderElement(someNode, this,
                           containerListIterator);
                 }
                else
                {*/
                firstChild = new NormalizedNodeElement(something, this,
                        dataiterator);
               // }
              }


            // construct a NodeBuilderElement, passing in the new node, the same
            // parent, and the iterator
            // cache the node builder element to first child
            isFirstInit = true;
        }
        return firstChild;
    }

    @Override
    public String getTextContent() {
        if (this.hasChildNodes()) {
            return "Text Content";
        }
        return node.getValue().toString();

    }
}
