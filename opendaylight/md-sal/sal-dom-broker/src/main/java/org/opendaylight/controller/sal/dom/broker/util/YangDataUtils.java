/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;

import static com.google.common.base.Preconditions.*;

public class YangDataUtils {

    public YangDataUtils() {
        // TODO Auto-generated constructor stub
    }

    
    
    public static Map<Map<QName,Object>,CompositeNode> toIndexMap(List<CompositeNode> nodes,List<QName> keys) {
        ConcurrentHashMap<Map<QName,Object>,CompositeNode> ret = new ConcurrentHashMap<>();
        for(CompositeNode node : nodes) {
            Map<QName, Object> key = getKeyMap(node,keys);
            ret.put(key, node);
        }
        return ret;
    }



    public static Map<QName,Object> getKeyMap(CompositeNode node, List<QName> keys) {
        Map<QName,Object> map = new HashMap<>();
        for(QName key : keys) {
            SimpleNode<?> keyNode = node.getFirstSimpleByName(QName.create(node.getNodeType(), key.getLocalName()));
            checkArgument(keyNode != null,"Node must contains all keys.");
            Object value = keyNode.getValue();
            map.put(key, value);
            
        }
        return map;
    }
}
