/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.StringUtil;

class MethodSerializer {

    static String toString(Method method) {
        StringBuilder build = new StringBuilder();
        Consumer<Modifier> appendWithSpace = string -> build.append(string).append(" ");

        if (method.getJavadoc() != null) {
            build.append(StringUtil.writeComment(method.getJavadoc(), true));
        }

        method.getAnnotations().forEach(build::append);

        build.append("    ");
        method.getVisibility().ifPresent(appendWithSpace);
        method.getModifiers().forEach(appendWithSpace);
        build.append(method.getReturnType()).append(" ");

        build.append(method.getName()).append("(");
        boolean firstParam = true;
        for (Field param : method.getParameters()) {
            if (!firstParam) {
                build.append(", ");
            }
            param.getModifiers().forEach(appendWithSpace);
            build.append(param.getType()).append(" ");
            build.append(param.getName());
            firstParam = false;
        }
        build.append(")");

        if (!method.getThrowsExceptions().isEmpty()) {
            build.append(" throws ");
            build.append(method.getThrowsExceptions().stream().collect(Collectors.joining(", ")));
        }

        Optional<String> body = method.getBody();
        if (!body.isPresent()) {
            build.append(";");
            build.append("\n");
        } else {
            build.append(" {");
            build.append("\n");
            build.append("        ");
            build.append(body.get());
            build.append("\n");
            build.append("    ");
            build.append("}");
            build.append("\n");
        }
        build.append("\n");
        return build.toString();
    }
}
