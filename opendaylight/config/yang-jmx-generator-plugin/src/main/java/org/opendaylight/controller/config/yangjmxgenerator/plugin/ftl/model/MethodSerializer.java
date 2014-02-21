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

        build.append("    " + "public ");
        for (String mod : method.getModifiers()) {
            build.append(mod + " ");
        }
        build.append(method.getReturnType() + " ");

        build.append(method.getName() + "(");
        for (Field param : method.getParameters()) {
            for (String mod : param.getModifiers()) {
                build.append(mod + " ");
            }
            build.append(param.getType() + " ");
            build.append(param.getName() + ", ");
        }
        if (method.getParameters().isEmpty()) {
            build.append(")");
        } else {
            build.deleteCharAt(build.length() - 1);
            build.deleteCharAt(build.length() - 1);
            build.append(')');
        }

        if (method instanceof MethodDeclaration) {
            build.append(";");
            build.append("\n");
        } else if (method instanceof MethodDefinition) {
            if (!((MethodDefinition) method).getThrowsExceptions()
                    .isEmpty()) {
                build.append(" throws ");
            }
            for (String ex : ((MethodDefinition) method)
                    .getThrowsExceptions()) {
                build.append(ex + " ");
            }
            build.append(" {");
            build.append("\n");
            build.append("        ");
            build.append(((MethodDefinition) method).getBody());
            build.append("\n");
            build.append("    ");
            build.append("}");
            build.append("\n");
        }
        build.append("\n");
        return build.toString();
    }
}
