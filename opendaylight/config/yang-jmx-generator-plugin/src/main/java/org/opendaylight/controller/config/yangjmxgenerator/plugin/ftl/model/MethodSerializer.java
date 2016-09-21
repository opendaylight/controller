/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.StringUtil;

class MethodSerializer {

    static String toString(Method method) {
        StringBuilder build = new StringBuilder();
        if (method.getJavadoc() != null) {
            build.append(StringUtil.writeComment(method.getJavadoc(), true));
        }

        for(Annotation a: method.getAnnotations()) {
            build.append(a);
        }

        build.append("    ");
        build.append(method.getVisibility()).append(" ");
        for (String mod : method.getModifiers()) {
            build.append(mod).append(" ");
        }
        build.append(method.getReturnType()).append(" ");

        build.append(method.getName()).append("(");
        boolean firstParam = true;
        for (Field param : method.getParameters()) {
            if (!firstParam) {
                build.append(", ");
            }
            for (String mod : param.getModifiers()) {
                build.append(mod).append(" ");
            }
            build.append(param.getType()).append(" ");
            build.append(param.getName());
            firstParam = false;
        }
        build.append(")");

        if (method instanceof MethodDeclaration) {
            build.append(";");
            build.append("\n");
        } else if (method instanceof MethodDefinition) {
            MethodDefinition definition = (MethodDefinition) method;
            if (!definition.getThrowsExceptions().isEmpty()) {
                build.append(" throws ");
            }
            for (String ex : definition.getThrowsExceptions()) {
                build.append(ex).append(" ");
            }
            build.append(" {");
            build.append("\n");
            build.append("        ");
            build.append(definition.getBody());
            build.append("\n");
            build.append("    ");
            build.append("}");
            build.append("\n");
        }
        build.append("\n");
        return build.toString();
    }
}
