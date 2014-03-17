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
        builder.append("protected final "
                + JmxAttribute.class.getCanonicalName() + " "
                + moduleField.getName() + "JmxAttribute = new "
                + JmxAttribute.class.getCanonicalName() + "(\""
                + moduleField.getAttributeName() + "\");");
        builder.append("\n");

        builder.append("     private ");
        for (String mod : moduleField.getModifiers()) {
            builder.append(mod + " ");
        }
        builder.append(moduleField.getType() + " ");
        builder.append(moduleField.getName());
        if (moduleField.getNullableDefault() != null) {
            builder.append(" = " + moduleField.getNullableDefault());
        }
        builder.append(";");

        if (moduleField.isDependent()) {
            String comment = moduleField.getDependency().isMandatory() ? "mandatory"
                    : "optional";
            builder.append(" // " + comment);
        }
        builder.append("\n");

        builder.append("\n");

        return builder.toString();
    }
}
