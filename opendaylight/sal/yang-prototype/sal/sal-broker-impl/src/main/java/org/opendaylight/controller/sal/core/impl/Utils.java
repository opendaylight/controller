/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.yang.common.QName;


public class Utils {

    public static <T> void addToMap(Map<QName, List<T>> map, QName key, T value) {
        List<T> list = map.get(key);
        if (list == null) {
            list = new ArrayList<T>();
            map.put(key, list);
        }
        list.add(value);
    }

    public static <T> void removeFromMap(Map<QName, List<T>> map, QName key,
            T value) {
        List<T> list = map.get(key);
        if (list == null) {
            return;
        }
        list.remove(value);
        if (list.isEmpty()) {
            map.remove(key);
        }
    }
}

