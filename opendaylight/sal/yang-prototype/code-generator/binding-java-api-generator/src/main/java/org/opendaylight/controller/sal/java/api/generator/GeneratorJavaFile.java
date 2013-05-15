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
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneratorJavaFile {

    private static final Logger log = LoggerFactory
            .getLogger(GeneratorJavaFile.class);
    private final CodeGenerator interfaceGenerator;
    private final ClassCodeGenerator classGenerator;
    private final Set<GeneratedType> genTypes;
    private final Set<GeneratedTransferObject> genTransferObjects;

    public GeneratorJavaFile(final CodeGenerator codeGenerator,
            final Set<GeneratedType> types) {
        this.interfaceGenerator = codeGenerator;
        this.genTypes = types;
        this.genTransferObjects = new HashSet<GeneratedTransferObject>();
        classGenerator = new ClassCodeGenerator();
    }

    public GeneratorJavaFile(final Set<GeneratedType> types,
            final Set<GeneratedTransferObject> genTransferObjects) {
        this.interfaceGenerator = new InterfaceGenerator();
        this.classGenerator = new ClassCodeGenerator();
        this.genTypes = types;
        this.genTransferObjects = genTransferObjects;
    }

    @Deprecated
    public List<File> generateToFile(String path) throws IOException {
        final List<File> result = new ArrayList<File>();

        for (GeneratedType genType : genTypes) {
            final String parentPath = generateParentPath(path,
                    genType.getPackageName());

            final File directory = new File(parentPath);
            final File genFile = generateTypeToJavaFile(directory, genType,
                    interfaceGenerator);
            
            if (genFile != null) {
                result.add(genFile);
            }
        }

        for (GeneratedTransferObject transferObject : genTransferObjects) {
            final String parentPath = generateParentPath(path,
                    transferObject.getPackageName());

            final File directory = new File(parentPath);
            final File genFile = generateTypeToJavaFile(directory,
                    transferObject, classGenerator);

            if (genFile != null) {
                result.add(genFile);
            }
        }
        return result;
    }

    public List<File> generateToFile(final File parentDirectory) throws IOException {
        final List<File> result = new ArrayList<File>();
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
    
    @Deprecated
    private String generateParentPath(String path, String pkg) {
        List<String> dirs = new ArrayList<String>();
        String pkgPath = "";
        if (pkg != null) {
            if (pkg.length() > 0) {
                if (pkg.contains(".")) {
                    String[] split = pkg.split("\\.");
                    for (String dir : split) {
                        dirs.add(dir);
                    }
                } else {
                    dirs.add(pkg);
                }
                for (int i = 0; i < dirs.size(); i++) {
                    if (i == 0) {
                        pkgPath += dirs.get(i);
                    } else {
                        pkgPath += File.separator + dirs.get(i);
                    }
                }
            }
        }
        String fullPath = "";
        if (path != null) {
            if (path.endsWith(File.separator)) {
                fullPath = path + pkgPath;
            } else {
                fullPath = path + File.separator + pkgPath;
            }
        } else {
            fullPath = pkgPath;
        }
        return fullPath;
    }
}
