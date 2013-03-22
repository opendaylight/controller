/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.java.api.generator;

import static org.opendaylight.controller.sal.java.api.generator.Constants.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.opendaylight.controller.sal.binding.model.api.CodeGenerator;
import org.opendaylight.controller.sal.binding.model.api.Constant;
import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;

public class InterfaceGenerator implements CodeGenerator {

    public Writer generate(GeneratedType type) throws IOException {
        Writer writer = new StringWriter();
        final List<Constant> constants = type.getConstantDefinitions();
        final List<MethodSignature> methods = type.getMethodDefinitions();
        final List<Enumeration> enums = type.getEnumDefintions();

        writer.write(GeneratorUtil.createIfcDeclarationWithPkgName(
                type.getPackageName(), type.getName(), ""));
        writer.write(NL);

        if (constants != null) {
            for (Constant c : constants) {
                writer.write(GeneratorUtil.createConstant(c, TAB) + NL);
            }
            writer.write(NL);
        }

        if (methods != null) {
            for (MethodSignature m : methods) {
                writer.write(GeneratorUtil.createMethodDeclaration(m, TAB) + NL);
            }
            writer.write(NL);
        }

        if (enums != null) {
            for (Enumeration e : enums) {
                writer.write(GeneratorUtil.createEnum(e, TAB) + NL);
            }
            writer.write(NL);
        }

        writer.write(RCB);

        return writer;
    }

}
