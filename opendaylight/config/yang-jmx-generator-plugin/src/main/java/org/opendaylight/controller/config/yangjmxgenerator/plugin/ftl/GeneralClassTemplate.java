/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl;

import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Constructor;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Header;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.MethodDefinition;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.TypeDeclaration;

public class GeneralClassTemplate extends AbstractFtlTemplate {

    private final List<Constructor> constructors;

    public GeneralClassTemplate(Header header, String packageName, String name,
            List<String> extendedClasses, List<String> implementedIfcs,
            List<Field> fields, List<MethodDefinition> methods) {
        this(header, packageName, name, extendedClasses, implementedIfcs,
                fields, methods, false, false, Collections
                        .<Constructor> emptyList());
    }

    public GeneralClassTemplate(Header header, String packageName, String name,
            List<String> extendedClasses, List<String> implementedIfcs,
            List<Field> fields, List<MethodDefinition> methods,
            boolean isAbstract, boolean isFinal, List<Constructor> constructors) {
        super(header, packageName, fields, methods, new TypeDeclaration(
                "class", name, checkCardinality(extendedClasses),
                implementedIfcs, isAbstract, isFinal));
        this.constructors = constructors;
    }

    static List<String> checkCardinality(List<String> extendedClass) {
        if (extendedClass.size() > 1) {
            throw new IllegalArgumentException(
                    "Class cannot have more than one super class, found: " + extendedClass);
        }
        return extendedClass;
    }

    public List<Constructor> getConstructors() {
        return constructors;
    }

}
