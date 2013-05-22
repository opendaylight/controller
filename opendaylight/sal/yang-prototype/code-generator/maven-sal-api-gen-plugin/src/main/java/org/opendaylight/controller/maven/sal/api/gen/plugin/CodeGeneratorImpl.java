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
import java.util.Set;

import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.generator.impl.BindingGeneratorImpl;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.java.api.generator.GeneratorJavaFile;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang2sources.spi.CodeGenerator;

public class CodeGeneratorImpl implements CodeGenerator {

    @Override
    public Collection<File> generateSources(SchemaContext context,
            File outputBaseDir, Set<String> yangModulesNames) throws IOException {

        final BindingGenerator bindingGenerator = new BindingGeneratorImpl();
        final List<Type> types = bindingGenerator.generateTypes(context);
        final Set<GeneratedType> typesToGenerate = new HashSet<GeneratedType>();
        final Set<GeneratedTransferObject> tosToGenerate = new HashSet<GeneratedTransferObject>();
        for (Type type : types) {
            if (type instanceof GeneratedTransferObject) {
                tosToGenerate.add((GeneratedTransferObject) type);
            } else if (type instanceof GeneratedType) {
                typesToGenerate.add((GeneratedType) type);
            }


        }

        final GeneratorJavaFile generator = new GeneratorJavaFile(typesToGenerate, tosToGenerate);
        return generator.generateToFile(outputBaseDir);
    }

}
