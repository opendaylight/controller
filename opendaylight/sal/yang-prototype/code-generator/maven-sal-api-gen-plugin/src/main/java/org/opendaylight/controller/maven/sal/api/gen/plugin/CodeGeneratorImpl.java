/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.maven.sal.api.gen.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.generator.impl.BindingGeneratorImpl;
import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.java.api.generator.GeneratorJavaFile;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang2sources.spi.CodeGenerator;

public final class CodeGeneratorImpl implements CodeGenerator {

    @Override
    public Collection<File> generateSources(SchemaContext context, File outputBaseDir, Set<Module> yangModules)
            throws IOException {
        if (outputBaseDir == null) {
            outputBaseDir = new File("target" + File.separator + "generated-sources" + File.separator + "maven-sal-api-gen");
        }

        final BindingGenerator bindingGenerator = new BindingGeneratorImpl();
        final List<Type> types = bindingGenerator.generateTypes(context, yangModules);
        final Set<GeneratedType> typesToGenerate = new HashSet<>();
        final Set<GeneratedTransferObject> tosToGenerate = new HashSet<>();
        final Set<Enumeration> enumsToGenerate = new HashSet<>();

        for (Type type : types) {
            if (type instanceof GeneratedTransferObject) {
                tosToGenerate.add((GeneratedTransferObject) type);
            } else if (type instanceof GeneratedType) {
                typesToGenerate.add((GeneratedType) type);
            } else if (type instanceof Enumeration) {
                enumsToGenerate.add((Enumeration) type);
            }
        }

        final GeneratorJavaFile generator = new GeneratorJavaFile(typesToGenerate, tosToGenerate, enumsToGenerate);

        return generator.generateToFile(outputBaseDir);
    }

    @Override
    public void setLog(Log log) {
        // use maven logging if necessary

    }

    @Override
    public void setAdditionalConfig(Map<String, String> additionalConfiguration) {
        // no additional config utilized
    }

    @Override
    public void setResourceBaseDir(File resourceBaseDir) {
        // no resource processing necessary
    }

    @Override
    public void setMavenProject(MavenProject project) {
        // no additional information needed
    }

}
