/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Annotation.Parameter;

class AnnotationSerializer {

    static String toString(Annotation annotation) {
        StringBuilder builder = new StringBuilder();
        builder.append("@");
        builder.append(annotation.getName());
        if (!annotation.getParams().isEmpty()) {
            builder.append("(");
            for (Parameter param : annotation.getParams()) {
                builder.append(param.getKey());
                builder.append(" = ");
                builder.append(fixString(param.getValue()));
                builder.append(", ");
            }
            builder.setCharAt(builder.length() - 2, ')');
        }
        builder.append("\n");
        return builder.toString();
    }

    private static String fixString(String value) {
        // TODO replace with compress single line if possible
        return value.replaceAll("\\r\\n|\\r|\\n", " ").replaceAll(" +", " ");
    }

}
