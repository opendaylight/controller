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
    private List<ListSchemaNode> allLists;
    private List<ContainerSchemaNode> allContainers;
    private List<LeafSchemaNode> allLeafs;
    private List<LeafListSchemaNode> allLeafLists;
    private List<DataSchemaNode> allChilds;
    
    public DataNodeIterator(final DataNodeContainer container) {
        if (container == null) {
            throw new IllegalArgumentException("Data Node Container MUST be specified and cannot be NULL!");
        }
        
        init();
        this.container = container;
        traverse(this.container);
    }
    
    private void init() {
        this.allContainers = new ArrayList<ContainerSchemaNode>();
        this.allLists = new ArrayList<ListSchemaNode>();
        this.allLeafs = new ArrayList<LeafSchemaNode>();
        this.allLeafLists = new ArrayList<LeafListSchemaNode>();
        this.allChilds = new ArrayList<DataSchemaNode>();
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
        if (dataNode == null) {
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
            return;
        }
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
