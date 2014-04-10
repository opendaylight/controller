package org.opendaylight.controller.datastore.infinispan.utils;

import org.infinispan.tree.Fqn;
import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;

public class InfinispanTreeWrapper {

    private static final String DATA = "___data___";

    public void writeValue(TreeCache treeCache, Fqn path, Object value){
        treeCache.put(path, DATA, value);
    }

    public Object readValue(Node node){
        return node.get(DATA);
    }

}
