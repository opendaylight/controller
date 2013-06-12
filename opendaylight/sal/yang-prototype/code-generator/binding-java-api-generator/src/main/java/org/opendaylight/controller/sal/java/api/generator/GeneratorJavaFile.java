/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.java.api.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.sal.binding.model.api.CodeGenerator;
import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GeneratorJavaFile {

    private static final Logger log = LoggerFactory
            .getLogger(GeneratorJavaFile.class);
    private final CodeGenerator interfaceGenerator;
    private final ClassCodeGenerator classGenerator;
    private final EnumGenerator enumGenerator;
    
    private final Set<GeneratedType> genTypes;
    private final Set<GeneratedTransferObject> genTransferObjects;
    private final Set<Enumeration> enumerations; 

    public GeneratorJavaFile(final CodeGenerator codeGenerator,
            final Set<GeneratedType> types) {
        this.interfaceGenerator = codeGenerator;
        this.genTypes = types;
        this.genTransferObjects = new HashSet<>();
        this.enumerations = new HashSet<>();
        this.classGenerator = new ClassCodeGenerator();
        this.enumGenerator = new EnumGenerator();
    }

    public GeneratorJavaFile(final Set<GeneratedType> types,
            final Set<GeneratedTransferObject> genTransferObjects,
            final Set<Enumeration> enumerations) {
        this.interfaceGenerator = new InterfaceGenerator();
        this.classGenerator = new ClassCodeGenerator();
        this.enumGenerator = new EnumGenerator();
        
        this.genTypes = types;
        this.genTransferObjects = genTransferObjects;
        this.enumerations = enumerations;
    }

    public List<File> generateToFile(final File parentDirectory) throws IOException {
        final List<File> result = new ArrayList<>();
        for (GeneratedType type : genTypes) {
            final File genFile = generateTypeToJavaFile(parentDirectory, type,
                    interfaceGenerator);

            if (genFile != null) {
                result.add(genFile);
            }
        }
        for (GeneratedTransferObject transferObject : genTransferObjects) {
            final File genFile = generateTypeToJavaFile(parentDirectory,
                    transferObject, classGenerator);

            if (genFile != null) {
                result.add(genFile);
            }
        }
        
        for (Enumeration enumeration : enumerations) {
            final File genFile = generateTypeToJavaFile(parentDirectory,
                    enumeration, enumGenerator);

            if (genFile != null) {
                result.add(genFile);
            }
        }
        
        return result;
    }

    private File generateTypeToJavaFile(final File parentDir, final Type type,
            final CodeGenerator generator) throws IOException {
        if (parentDir == null) {
            log.warn("Parent Directory not specified, files will be generated "
                    + "accordingly to generated Type package path.");
        }
        if (type == null) {
            log.error("Cannot generate Type into Java File because " +
            		"Generated Type is NULL!");
            throw new IllegalArgumentException("Generated Type Cannot be NULL!");
        }
        if (generator == null) {
            log.error("Cannot generate Type into Java File because " +
            		"Code Generator instance is NULL!");
            throw new IllegalArgumentException("Code Generator Cannot be NULL!");
        }
        final File packageDir = packageToDirectory(parentDir,
                type.getPackageName());

        if (!packageDir.exists()) {
            packageDir.mkdirs();
        }
        final File file = new File(packageDir, type.getName() + ".java");
        try (final FileWriter fw = new FileWriter(file)) {
            file.createNewFile();

            try (final BufferedWriter bw = new BufferedWriter(fw)) {
                Writer writer = generator.generate(type);
                bw.write(writer.toString());
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new IOException(e.getMessage());
        }
        return file;
    }

    private File packageToDirectory(final File parentDirectory,
            final String packageName) {
        if (packageName == null) {
            throw new IllegalArgumentException("Package Name cannot be NULL!");
        }

        final String[] subDirNames = packageName.split("\\.");
        final StringBuilder dirPathBuilder = new StringBuilder();
        dirPathBuilder.append(subDirNames[0]);
        for (int i = 1; i < subDirNames.length; ++i) {
            dirPathBuilder.append(File.separator);
            dirPathBuilder.append(subDirNames[i]);
        }
        return new File(parentDirectory, dirPathBuilder.toString());
    }
}
