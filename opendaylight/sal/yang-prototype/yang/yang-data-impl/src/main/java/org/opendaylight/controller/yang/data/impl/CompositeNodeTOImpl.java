/**
 * 
 */
package org.opendaylight.controller.yang.data.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.SimpleNode;

/**
 * @author michal.rehak
 * 
 */
public class CompositeNodeTOImpl extends AbstractNodeTO<List<Node<?>>>
        implements CompositeNode {

    private Map<QName, List<Node<?>>> nodeMap;

    /**
     * @param qname
     * @param parent use null to create top composite node (without parent)
     * @param value
     */
    public CompositeNodeTOImpl(QName qname, CompositeNode parent,
            List<Node<?>> value) {
        super(qname, parent, value);
        if (value != null) {
            nodeMap = NodeUtils.buildNodeMap(getValue());
        }
    }
    

    /**
     * @return the nodeMap
     */
    protected Map<QName, List<Node<?>>> getNodeMap() {
        return nodeMap;
    }
    
    @Override
    public List<Node<?>> getChildren() {
        return getValue();
    }

    @Override
    public SimpleNode<?> getFirstSimpleByName(QName leafQName) {
        List<SimpleNode<?>> list = getSimpleNodesByName(leafQName);
        if (list.isEmpty())
            return null;
        return list.get(0);
    }

    @Override
    public List<CompositeNode> getCompositesByName(QName children) {
        List<Node<?>> toFilter = getNodeMap().get(children);
        List<CompositeNode> list = new ArrayList<CompositeNode>();
        for (Node<?> node : toFilter) {
            if (node instanceof CompositeNode)
                list.add((CompositeNode) node);
        }
        return list;
    }

    @Override
    public List<SimpleNode<?>> getSimpleNodesByName(QName children) {
        List<Node<?>> toFilter = getNodeMap().get(children);
        List<SimpleNode<?>> list = new ArrayList<SimpleNode<?>>();

        for (Node<?> node : toFilter) {
            if (node instanceof SimpleNode<?>)
                list.add((SimpleNode<?>) node);
        }
        return list;
    }

    @Override
    public CompositeNode getFirstCompositeByName(QName container) {
        List<CompositeNode> list = getCompositesByName(container);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    /**
     * @param leaf
     * @return TODO:: do we need this method?
     */
    public SimpleNode<?> getFirstLeafByName(QName leaf) {
        List<SimpleNode<?>> list = getSimpleNodesByName(leaf);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public List<CompositeNode> getCompositesByName(String children) {
        return getCompositesByName(localQName(children));
    }
    
    @Override
    public List<SimpleNode<?>> getSimpleNodesByName(String children) {
        return getSimpleNodesByName(localQName(children));
    }

    private QName localQName(String str) {
        return new QName(getNodeType(), str);
    }

}
