/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang2sources.plugin;

import java.io.File;

import com.google.common.base.Preconditions;

/**
 * Base complex configuration arguments
 */
public abstract class ConfigArg {
    public static final String CODE_GEN_DEFAULT_DIR = "code-generator-files/";
    public static final String RESOURCE_GEN_DEFAULT_DIR = "resource-generator-files/";

    protected File outputBaseDir;

    public ConfigArg(File outputBaseDir) {
        this.outputBaseDir = outputBaseDir;
    }

    public ConfigArg() {
    }

    public File getOutputBaseDir() {
        return outputBaseDir;
    }

    public abstract void check();

    /**
     * Configuration argument for resource generator class and output directory.
     */
    public static final class ResourceProviderArg extends ConfigArg {
        private String resourceProviderClass;

        public ResourceProviderArg() {
        }

        public ResourceProviderArg(String resourceProviderClass) {
            this(resourceProviderClass, new File(RESOURCE_GEN_DEFAULT_DIR));
        }

        public ResourceProviderArg(String resourceProviderClass,
                File outputBaseDir) {
            super(outputBaseDir);
            this.resourceProviderClass = resourceProviderClass;
        }

        @Override
        public void check() {
            Preconditions
                    .checkNotNull(resourceProviderClass,
                            "resourceProviderClass for ResourceProvider cannot be null");
        }

        public String getResourceProviderClass() {
            return resourceProviderClass;
        }
    }

    /**
     * Configuration argument for code generator class and output directory.
     */
    public static final class CodeGeneratorArg extends ConfigArg {
        private String codeGeneratorClass;

        public CodeGeneratorArg() {
            super(new File(CODE_GEN_DEFAULT_DIR));
        }

        public CodeGeneratorArg(String codeGeneratorClass) {
            this(codeGeneratorClass, new File(CODE_GEN_DEFAULT_DIR));
        }

        public CodeGeneratorArg(String codeGeneratorClass, File outputBaseDir) {
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
    }
}