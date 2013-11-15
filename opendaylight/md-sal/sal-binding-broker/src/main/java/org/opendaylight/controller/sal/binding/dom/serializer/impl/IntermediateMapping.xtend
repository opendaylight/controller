package org.opendaylight.controller.sal.binding.dom.serializer.impl

import org.opendaylight.yangtools.yang.data.api.Node
import java.util.Map
import org.opendaylight.yangtools.yang.common.QName
import java.util.List
import java.util.ArrayList
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl
import com.google.common.base.Preconditions

class IntermediateMapping {
    
    
    
    static def Node<?> toNode(Map map) {
        val nodeMap = map as Map<QName,Object>;
        Preconditions.checkArgument(map.size == 1);
        val elem = nodeMap.entrySet.iterator.next;
        val qname = elem.key;
        val value = elem.value;
        toNodeImpl(qname, value);
    }

    static def dispatch Node<?> toNodeImpl(QName name, List<?> objects) {
        val values = new ArrayList<Node<?>>(objects.size);
        for (obj : objects) {
            values.add(toNode(obj as Map));
        }
        return new CompositeNodeTOImpl(name, null, values);
    }

    static def dispatch Node<?> toNodeImpl(QName name, Map<QName, Object> object) {
        throw new UnsupportedOperationException("Unsupported node hierarchy.");
    }

    static def dispatch Node<?> toNodeImpl(QName name, Object object) {
        return new SimpleNodeTOImpl(name, null, object);
    } 
}