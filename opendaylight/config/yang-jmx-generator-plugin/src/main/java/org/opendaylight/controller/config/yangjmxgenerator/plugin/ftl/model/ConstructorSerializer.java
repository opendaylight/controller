/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

public class ConstructorSerializer {

    public static String toString(Constructor constr) {
        StringBuilder build = new StringBuilder();
        build.append("    ");
        if (constr.isPublic()) {
            build.append("public ");
        }
        build.append(constr.getTypeName());
        build.append("() {");
        build.append("\n");
        build.append("    ");
        build.append("    ");
        build.append(constr.getBody());
        build.append("\n");
        build.append("    ");
        build.append("}");
        build.append("\n");
        build.append("\n");
        return build.toString();
    }
}
