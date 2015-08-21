/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

import org.opendaylight.controller.config.api.JmxAttribute;

public class ModuleFieldSerializer {


    public static String toString(ModuleField moduleField) {
        StringBuilder builder = new StringBuilder();
        builder.append("    ");
        builder.append("public static final ");
        builder.append(JmxAttribute.class.getCanonicalName());
        builder.append(" ");
        builder.append(moduleField.getName());
        builder.append("JmxAttribute = new ");
        builder.append(JmxAttribute.class.getCanonicalName());
        builder.append("(\"");
        builder.append(moduleField.getAttributeName());
        builder.append("\");");
        builder.append("\n");

        builder.append("     private ");
        for (String mod : moduleField.getModifiers()) {
            builder.append(mod).append(" ");
        }
        builder.append(moduleField.getType()).append(" ");
        builder.append(moduleField.getName());
        if (moduleField.getNullableDefault() != null) {
            builder.append(" = ").append(moduleField.getNullableDefault());
        }
        builder.append(";");

        if (moduleField.isDependent()) {
            String comment = moduleField.getDependency().isMandatory() ? "mandatory"
                    : "optional";
            builder.append(" // ").append(comment);
        }
        builder.append("\n");

        builder.append("\n");

        return builder.toString();
    }
}
