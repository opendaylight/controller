/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.java.api.generator;

import static org.opendaylight.controller.sal.java.api.generator.Constants.NL;
import static org.opendaylight.controller.sal.java.api.generator.Constants.RCB;
import static org.opendaylight.controller.sal.java.api.generator.Constants.TAB;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.opendaylight.controller.sal.binding.model.api.CodeGenerator;
import org.opendaylight.controller.sal.binding.model.api.GeneratedProperty;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.Type;

public class CompositeKeyGenerator implements CodeGenerator {

    @Override
    public Writer generate(Type type) throws IOException {
        final Writer writer = new StringWriter();
        if (type instanceof GeneratedTransferObject) {
            GeneratedTransferObject genTO = (GeneratedTransferObject)type;
            final List<GeneratedProperty> fields = genTO.getProperties();

            writer.write(GeneratorUtil.createClassDeclarationWithPkgName(
                    type.getPackageName(), type.getName(), ""));
            writer.write(NL);
            writer.write(NL);
            
            if (fields != null) {
                for (GeneratedProperty field : fields) {
                    writer.write(GeneratorUtil.createField(field, TAB) + NL);
                }
                writer.write(NL);
                writer.write(GeneratorUtil.createConstructor(genTO, TAB) + NL);
                writer.write(NL);
                for (GeneratedProperty field : fields) {
                    writer.write(GeneratorUtil.createGetter(field, TAB) + NL);
                }
                writer.write(NL);

                writer.write(GeneratorUtil.createHashCode(genTO.getHashCodeIdentifiers(), TAB) + NL);
                writer.write(GeneratorUtil.createEquals(genTO, genTO.getEqualsIdentifiers(), TAB) + NL);
                writer.write(GeneratorUtil.createToString(genTO, genTO.getToStringIdentifiers(), TAB) + NL);

                writer.write(RCB);
            }
        }
        return writer;
    }

}
