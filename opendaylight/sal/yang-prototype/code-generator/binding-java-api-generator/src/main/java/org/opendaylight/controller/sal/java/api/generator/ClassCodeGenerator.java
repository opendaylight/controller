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
import java.util.Map;

import org.opendaylight.controller.sal.binding.model.api.*;

public final class ClassCodeGenerator implements CodeGenerator {

    private Map<String, String> imports;

    private void generatePackage(Writer writer, String packageName) throws IOException {
        writer.write(GeneratorUtil.createPackageDeclaration(packageName));
        writer.write(NL);
    }

    private void generateImports(Writer writer) throws IOException {
        List<String> importLines = GeneratorUtil.createImportLines(imports, null);
        for (String line : importLines) {
            writer.write(line + NL);
        }
        writer.write(NL);
    }

    private void generateClassBody(Writer writer, GeneratedTransferObject genTO, String packageName, String indent,
            boolean isInnerClass) throws IOException {
        final List<GeneratedProperty> fields = genTO.getProperties();
        final List<Enumeration> enums = genTO.getEnumerations();
        final List<Constant> consts = genTO.getConstantDefinitions();

        writer.write(GeneratorUtil.createClassDeclaration(genTO, indent, imports, genTO.isAbstract(), isInnerClass));
        writer.write(NL);
        writer.write(NL);

        if (consts != null) {
            for (Constant con : consts) {
                writer.write(GeneratorUtil.createConstant(con, indent + TAB, imports, packageName));
                writer.write(NL);
            }
        }

        if (enums != null) {
            EnumGenerator enumGenerator = new EnumGenerator();
            for (Enumeration e : enums) {
                writer.write(enumGenerator.generateInnerEnumeration(e, indent + TAB).toString());
                writer.write(NL);
            }
        }

        writer.write(GeneratorUtil.createStaticInicializationBlock(genTO, indent + TAB));

        if (fields != null) {
            for (GeneratedProperty field : fields) {
                writer.write(GeneratorUtil.createField(field, indent + TAB, imports, packageName) + NL);
            }

            writer.write(NL);
            final boolean oneConstructor;
            if (genTO.isUnionType()) {
                oneConstructor = false;
            } else {
                oneConstructor = true;
            }
            writer.write(GeneratorUtil.createConstructor(genTO, indent + TAB, imports, genTO.isAbstract(),
                    oneConstructor));
            writer.write(NL);

            for (GeneratedProperty field : fields) {
                writer.write(GeneratorUtil.createGetter(field, indent + TAB, imports, packageName) + NL);
                if (!field.isReadOnly()) {
                    writer.write(GeneratorUtil.createSetter(field, indent + TAB, imports, packageName) + NL);
                }
            }

            if (!genTO.getHashCodeIdentifiers().isEmpty()) {
                writer.write(GeneratorUtil.createHashCode(genTO.getHashCodeIdentifiers(), indent + TAB) + NL);
            }

            if (!genTO.getEqualsIdentifiers().isEmpty()) {
                writer.write(GeneratorUtil.createEquals(genTO, genTO.getEqualsIdentifiers(), indent + TAB) + NL);
            }

            if (!genTO.getToStringIdentifiers().isEmpty()) {
                writer.write(GeneratorUtil.createToString(genTO, genTO.getToStringIdentifiers(), indent + TAB) + NL);
            }

            writer.write(indent + RCB);
        }
    }

    @Override
    public Writer generate(Type type) throws IOException {
        final Writer writer = new StringWriter();

        if (type instanceof GeneratedTransferObject) {
            GeneratedTransferObject genTO = (GeneratedTransferObject) type;
            imports = GeneratorUtil.createImports(genTO);

            final String currentPkg = genTO.getPackageName();

            generatePackage(writer, currentPkg);

            generateImports(writer);

            generateClassBody(writer, genTO, currentPkg, NO_INDENT, false);

        }
        return writer;
    }

    public Writer generateOnlyClass(Type type, Map<String, String> imports) throws IOException {
        this.imports = imports;
        Writer writer = new StringWriter();

        if (type instanceof GeneratedTransferObject) {
            GeneratedTransferObject genTO = (GeneratedTransferObject) type;

            final String currentPkg = "";

            generateClassBody(writer, genTO, currentPkg, TAB, true);

        }

        return writer;
    }
}
