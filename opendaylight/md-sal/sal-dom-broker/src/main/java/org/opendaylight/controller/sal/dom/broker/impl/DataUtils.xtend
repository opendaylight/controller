package org.opendaylight.controller.sal.dom.broker.impl

import org.opendaylight.yangtools.yang.data.api.CompositeNode
import java.util.Map
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import java.util.Map.Entry
import java.util.HashSet
import java.util.ArrayList
import org.opendaylight.yangtools.yang.data.api.Node
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl

class DataUtils {

    static def CompositeNode read(Map<InstanceIdentifier, CompositeNode> map, InstanceIdentifier path) {
        val root = map.get(path);
        val childs = map.getChilds(path);
        if(root === null && childs.empty) {
            return null;
        }
        
        return merge(path, root, childs);
    }

    static def CompositeNode merge(InstanceIdentifier path, CompositeNode node,
        HashSet<Entry<InstanceIdentifier, CompositeNode>> entries) {
        val it = new ArrayList<Node<?>>();
        val qname = path.path.last.nodeType;
        if (node != null) {
            addAll(node.children);
        }
        for (entry : entries) {
            val nesting = entry.key.path.size - path.path.size;
            if (nesting === 1) {
                add(entry.value);
            }
        }
        return new CompositeNodeTOImpl(qname, null, it);
    }

    static def getChilds(Map<InstanceIdentifier, CompositeNode> map, InstanceIdentifier path) {
        val it = new HashSet<Entry<InstanceIdentifier, CompositeNode>>();
        for (entry : map.entrySet) {
            if (path.contains(entry.key)) {
                add(entry);
            }
        }
        return it;
    }

}
