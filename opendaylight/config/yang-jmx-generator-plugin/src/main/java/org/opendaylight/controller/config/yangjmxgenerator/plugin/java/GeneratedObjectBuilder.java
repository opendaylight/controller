/*
 * Copyright (c) 2013, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.java;

import static org.opendaylight.controller.config.yangjmxgenerator.plugin.util.StringUtil.prefixAndJoin;

import com.google.common.base.Optional;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.StringUtil;

public class GeneratedObjectBuilder {
    private final JavaFileInput input;

    public GeneratedObjectBuilder(JavaFileInput input) {
        this.input = input;
    }


    public GeneratedObject toGeneratedObject() {
        FullyQualifiedName fqn = input.getFQN();
        StringBuilder content = new StringBuilder();


        content.append(maybeAddComment(input.getCopyright()));
        content.append(maybeAddComment(input.getHeader()));

        if (!input.getFQN().getPackageName().isEmpty()) {
            content.append("package ");
            content.append(input.getFQN().getPackageName());
            content.append(";\n");
        }
        content.append(maybeAddComment(input.getClassJavaDoc(), true));

        for (String classAnnotation : input.getClassAnnotations()) {
            content.append(classAnnotation);
            content.append("\n");
        }

        content.append("public ");
        content.append(input.getType());
        content.append(" ");
        content.append(input.getFQN().getTypeName());
        content.append(prefixAndJoin(input.getExtends(), "extends"));
        content.append(prefixAndJoin(input.getImplements(), "implements"));
        content.append(" {\n");

        for (String method : input.getBodyElements()) {
            content.append(method);
            content.append("\n");
        }

        content.append("\n}\n");

        return new GeneratedObject(fqn, content.toString());
    }

    private static String maybeAddComment(Optional<String> comment) {
        return maybeAddComment(comment, false);
    }

    private static String maybeAddComment(Optional<String> comment, boolean isJavadoc) {

        if (comment.isPresent()) {
            String input = comment.get();
            return StringUtil.writeComment(input, isJavadoc);
        } else {
            return "";
        }
    }

}
