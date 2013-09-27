/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl;

class TypeHelper {

    /**
     * Output string representing java notation of generic class, e.g.
     * "List<String>" for input parameters List.class, String.class
     */
    static String getGenericType(Class<?> type, Class<?>... parameters) {
        StringBuffer sb = new StringBuffer();
        sb.append(type.getCanonicalName());
        if (parameters.length > 0) {
            sb.append("<");
            boolean first = true;
            for (Class<?> parameter : parameters) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(parameter.getCanonicalName());
            }
            sb.append(">");
        }
        return sb.toString();
    }
}
