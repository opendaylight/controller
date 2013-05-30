/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang2sources.plugin;

import java.io.File;
import java.util.Map;

import org.apache.maven.project.MavenProject;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * Base complex configuration arguments
 */
public abstract class ConfigArg {

    private final File outputBaseDir;

    public ConfigArg(String outputBaseDir) {
        this.outputBaseDir = new File(outputBaseDir);
    }

    public File getOutputBaseDir(MavenProject project) {
        if (outputBaseDir.isAbsolute()) {
            return outputBaseDir;
        } else {
            return new File(project.getBasedir(), outputBaseDir.getPath());
        }
    }

    public abstract void check();

    /**
     * Configuration argument for code generator class and output directory.
     */
    public static final class CodeGeneratorArg extends ConfigArg {
        private static final String CODE_GEN_DEFAULT_DIR = "target"
                + File.separator + "generated-sources";
        private String codeGeneratorClass;

        private Map<String, String> additionalConfiguration = Maps.newHashMap();

        public CodeGeneratorArg() {
            super(CODE_GEN_DEFAULT_DIR);
        }

        public CodeGeneratorArg(String codeGeneratorClass) {
            this(codeGeneratorClass, CODE_GEN_DEFAULT_DIR);
        }

        public CodeGeneratorArg(String codeGeneratorClass, String outputBaseDir) {
            super(outputBaseDir);
            this.codeGeneratorClass = codeGeneratorClass;
        }

        @Override
        public void check() {
            Preconditions.checkNotNull(codeGeneratorClass,
                    "codeGeneratorClass for CodeGenerator cannot be null");
        }

        public String getCodeGeneratorClass() {
            return codeGeneratorClass;
        }

        public Map<String, String> getAdditionalConfiguration() {
            return additionalConfiguration;
        }
    }
}
