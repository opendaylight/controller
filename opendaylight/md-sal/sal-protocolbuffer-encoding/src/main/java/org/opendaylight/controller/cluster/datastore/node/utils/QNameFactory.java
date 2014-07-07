package org.opendaylight.controller.cluster.datastore.node.utils;

import org.opendaylight.yangtools.yang.common.QName;

import java.util.HashMap;
import java.util.Map;

public class QNameFactory {
    private static final Map<String, QName> cache = new HashMap<>();

    public static QName create(String name){
        QName value = cache.get(name);
        if(value == null){
            synchronized (cache){
                value = cache.get(name);
                if(value == null) {
                    value = QName.create(name);
                    cache.put(name, value);
                }
            }
        }
        return value;
    }
}
