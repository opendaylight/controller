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
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;

public class CompositeKeyGenerator implements CodeGenerator {

    @Override
    public Writer generate(GeneratedType type) throws IOException {
        final Writer writer = new StringWriter();
        final List<Constant> fields = type.getConstantDefinitions();

        writer.write(GeneratorUtil.createClassDeclarationWithPkgName(
                type.getPackageName(), type.getName(), ""));
        writer.write(NL);
        writer.write(NL);

        if (fields != null) {
            for (Constant field : fields) {
                writer.write(GeneratorUtil.createField(field, TAB) + NL);
            }
            writer.write(NL);

            for (Constant field : fields) {
                writer.write(GeneratorUtil.createGetter(field, TAB) + NL);
            }
            writer.write(NL);

            writer.write(GeneratorUtil.createHashCode(fields, TAB) + NL);
            writer.write(GeneratorUtil.createEquals(type, fields, TAB) + NL);
            writer.write(GeneratorUtil.createToString(type, fields, TAB) + NL);

            writer.write(RCB);
        }

        return writer;
    }

}
