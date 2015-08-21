/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

public class FieldSerializer {

    public static String toString(Field field) {
        StringBuilder build = new StringBuilder();
        build.append("private ");
        for (String mod : field.getModifiers()) {
            build.append(mod).append(" ");
        }
        build.append(field.getType()).append(" ");
        build.append(field.getName());
        if (field.getDefinition() != null) {
            build.append(" = ").append(field.getDefinition());
        }
        build.append(";");
        build.append("\n");
        return build.toString();
    }
}
