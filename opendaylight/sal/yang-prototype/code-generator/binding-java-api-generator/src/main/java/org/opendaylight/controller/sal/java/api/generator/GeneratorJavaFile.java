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

public class GeneratorJavaFile {

    private final CodeGenerator interfaceGenerator;
    private final CompositeKeyGenerator classGenerator;
    private final Set<GeneratedType> types;
    private final Set<GeneratedTransferObject> genTransferObjects;

    public GeneratorJavaFile(final CodeGenerator codeGenerator,
            final Set<GeneratedType> types) {
        this.interfaceGenerator = codeGenerator;
        this.types = types;
        this.genTransferObjects = new HashSet<GeneratedTransferObject>();
        classGenerator = new CompositeKeyGenerator();
    }

    public GeneratorJavaFile(final Set<GeneratedType> types,
            final Set<GeneratedTransferObject> genTransferObjects) {
        this.interfaceGenerator = new InterfaceGenerator();
        this.classGenerator = new CompositeKeyGenerator();
        this.types = types;
        this.genTransferObjects = genTransferObjects;
    }

    public boolean generateToFile() {
        return generateToFile(null);
    }

    public boolean generateToFile(String path) {
        try {
            for (GeneratedType type : types) {
                String parentPath = generateParentPath(path,
                        type.getPackageName());

                File file = new File(parentPath, type.getName() + ".java");
                File parent = file.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }

                if (!file.exists()) {
                    FileWriter fw = null;
                    BufferedWriter bw = null;

                    file.createNewFile();
                    fw = new FileWriter(file);
                    bw = new BufferedWriter(fw);
                    Writer writer = interfaceGenerator.generate(type);
                    bw.write(writer.toString());

                    if (bw != null) {
                        try {
                            bw.close();
                        } catch (IOException e) {
                            // TODO: log?
                        }
                    }
                }
            }
            for (GeneratedTransferObject transferObject : genTransferObjects) {
                String parentPath = generateParentPath(path,
                        transferObject.getPackageName());

                File file = new File(parentPath, transferObject.getName() + ".java");
                File parent = file.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }

                if (!file.exists()) {
                    FileWriter fw = null;
                    BufferedWriter bw = null;

                    file.createNewFile();
                    fw = new FileWriter(file);
                    bw = new BufferedWriter(fw);
                    Writer writer = classGenerator.generate(transferObject);
                    bw.write(writer.toString());

                    if (bw != null) {
                        try {
                            bw.close();
                        } catch (IOException e) {
                            // TODO: log?
                        }
                    }
                }
            }
            return true;
        } catch (IOException e) {
            // TODO: log?
            return false;
        }
    }

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
