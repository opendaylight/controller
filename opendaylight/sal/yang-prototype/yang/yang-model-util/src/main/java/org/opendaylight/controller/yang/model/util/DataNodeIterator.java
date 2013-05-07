package org.opendaylight.controller.yang.model.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.yang.model.api.ContainerSchemaNode;
import org.opendaylight.controller.yang.model.api.DataNodeContainer;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafListSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;

public class DataNodeIterator implements Iterator<DataSchemaNode> {
    
    private final DataNodeContainer container;
    private final List<ListSchemaNode> allLists;
    private final List<ContainerSchemaNode> allContainers;
    private final List<LeafSchemaNode> allLeafs;
    private final List<LeafListSchemaNode> allLeafLists;
    private final List<DataSchemaNode> allChilds;
    
    public DataNodeIterator(final DataNodeContainer container) {
        if (container == null) {
            throw new IllegalArgumentException("Data Node Container MUST be specified!");
        }
        
        this.allContainers = new ArrayList<ContainerSchemaNode>();
        this.allLists = new ArrayList<ListSchemaNode>();
        this.allLeafs = new ArrayList<LeafSchemaNode>();
        this.allLeafLists = new ArrayList<LeafListSchemaNode>();
        this.allChilds = new ArrayList<DataSchemaNode>();
        
        this.container = container;
        
        traverse(this.container);
    }
    
    public List<ContainerSchemaNode> allContainers() {
        return allContainers;
    }
    
    public List<ListSchemaNode> allLists() {
        return allLists;
    }
    
    public List<LeafSchemaNode> allLeafs() {
        return allLeafs;
    }
    
    public List<LeafListSchemaNode> allLeafLists() {
        return allLeafLists;
    }
    
    private void traverse(final DataNodeContainer dataNode) {
        if (!containChildDataNodeContainer(dataNode)) {
            return;
        }

        final Set<DataSchemaNode> childs = dataNode.getChildNodes();
        if (childs != null) {
            for (DataSchemaNode childNode : childs) {
                if (childNode.isAugmenting()) {
                    continue;
                }
                allChilds.add(childNode);
                if (childNode instanceof ContainerSchemaNode) {
                    final ContainerSchemaNode container = (ContainerSchemaNode) childNode;
                    allContainers.add(container);
                    traverse(container);
                } else if (childNode instanceof ListSchemaNode) {
                    final ListSchemaNode list = (ListSchemaNode) childNode;
                    allLists.add(list);
                    traverse(list);
                } else if (childNode instanceof LeafSchemaNode) {
                    final LeafSchemaNode leaf = (LeafSchemaNode) childNode;
                    allLeafs.add(leaf);
                } else if (childNode instanceof LeafListSchemaNode) {
                    final LeafListSchemaNode leafList = (LeafListSchemaNode) childNode;
                    allLeafLists.add(leafList);
                }
            }
        }
    }

    /**
     * Returns <code>true</code> if and only if the child node contain at least
     * one child container schema node or child list schema node, otherwise will
     * always returns <code>false</code>
     * 
     * @param container
     * @return <code>true</code> if and only if the child node contain at least
     *         one child container schema node or child list schema node,
     *         otherwise will always returns <code>false</code>
     */
    private boolean containChildDataNodeContainer(
            final DataNodeContainer container) {
        if (container != null) {
            final Set<DataSchemaNode> childs = container.getChildNodes();
            if ((childs != null) && (childs.size() > 0)) {
                for (final DataSchemaNode childNode : childs) {
                    if (childNode instanceof DataNodeContainer) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    @Override
    public boolean hasNext() {
        if (container.getChildNodes() != null) {
            Set<DataSchemaNode> childs = container.getChildNodes();
            
            if ((childs != null) && !childs.isEmpty()) {
                return childs.iterator().hasNext();
            }
        }
        return false;
    }

    @Override
    public DataSchemaNode next() {
        return allChilds.iterator().next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
