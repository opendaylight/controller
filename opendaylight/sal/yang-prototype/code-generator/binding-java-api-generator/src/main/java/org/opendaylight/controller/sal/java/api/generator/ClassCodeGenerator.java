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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.binding.model.api.CodeGenerator;
import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.GeneratedProperty;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferIdentityObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.Type;

public final class ClassCodeGenerator implements CodeGenerator {

    private Map<String, LinkedHashMap<String, Integer>> imports;

    @Override
    public Writer generate(Type type) throws IOException {
        final Writer writer = new StringWriter();

        if (type instanceof GeneratedTransferObject) {
            GeneratedTransferObject genTO = (GeneratedTransferObject) type;            
            imports = GeneratorUtil.createImports(genTO);
            
            final String currentPkg = genTO.getPackageName();
            final List<GeneratedProperty> fields = genTO.getProperties();
            final List<Enumeration> enums = genTO.getEnumerations();

            writer.write(GeneratorUtil.createPackageDeclaration(currentPkg));
            writer.write(NL);

            List<String> importLines = GeneratorUtil.createImportLines(imports);
            for (String line : importLines) {
                writer.write(line + NL);
            }
            writer.write(NL);

            writer.write(GeneratorUtil.createClassDeclaration(genTO, "",
                    imports, genTO.isAbstract()));
            writer.write(NL);
            writer.write(NL);
            
            if (enums != null) {           
               	EnumGenerator enumGenerator = new EnumGenerator();
            	for ( Enumeration e : enums ) {            		
            		writer.write(enumGenerator.generateInnerEnumeration(e, TAB).toString());
            		writer.write(NL);
            	}
            }

            if (fields != null) {
                for (GeneratedProperty field : fields) {
                    writer.write(GeneratorUtil.createField(field, TAB, imports,
                            currentPkg) + NL);
                }
                writer.write(NL);
                writer.write(GeneratorUtil.createConstructor(genTO, TAB,
                        imports, genTO.isAbstract()) + NL);
                writer.write(NL);
                for (GeneratedProperty field : fields) {
                    writer.write(GeneratorUtil.createGetter(field, TAB,
                            imports, currentPkg) + NL);
                    if (!field.isReadOnly()) {
                        writer.write(GeneratorUtil.createSetter(field, TAB,
                                imports, currentPkg) + NL);
                    }
                }
                writer.write(NL);

                if (!genTO.getHashCodeIdentifiers().isEmpty()) {
                    writer.write(GeneratorUtil.createHashCode(
                            genTO.getHashCodeIdentifiers(), TAB)
                            + NL);
                }

                if (!genTO.getEqualsIdentifiers().isEmpty()) {
                    writer.write(GeneratorUtil.createEquals(genTO,
                            genTO.getEqualsIdentifiers(), TAB)
                            + NL);
                }

                if (!genTO.getToStringIdentifiers().isEmpty()) {
                    writer.write(GeneratorUtil.createToString(genTO,
                            genTO.getToStringIdentifiers(), TAB)
                            + NL);

                }

                writer.write(RCB);
            }
        }
        return writer;
    }

}
