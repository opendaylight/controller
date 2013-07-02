/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.java.api.generator;

import static org.opendaylight.controller.sal.java.api.generator.Constants.NL;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.opendaylight.controller.sal.binding.model.api.CodeGenerator;
import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.Type;

public class EnumGenerator implements CodeGenerator {

    @Override
    public Writer generate(Type type) throws IOException {
        final Writer writer = new StringWriter();

        if (type instanceof Enumeration) {
            Enumeration enums = (Enumeration) type;
            writer.write(GeneratorUtil.createPackageDeclaration(enums.getPackageName()));
            writer.write(NL + NL);
            writer.write(GeneratorUtil.createEnum(enums, ""));
        }

        return writer;
    }

    public Writer generateInnerEnumeration(Type type, String indent) throws IOException {
        final Writer writer = new StringWriter();

        if (type instanceof Enumeration) {
            Enumeration enums = (Enumeration) type;
            writer.write(GeneratorUtil.createEnum(enums, indent));
        }

        return writer;
    }

}
