/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.api;

import java.io.InputStream;
import java.util.Set;

import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.api.type.UnknownTypeDefinition;

/**
 * Yang Model Parser interface is designed for parsing yang models and convert
 * the information to Data Schema Tree.
 *
 */
public interface YangModelParser {

    /**
     * Parse single Yang model file and return the schema definition of Yang
     * module defined in *.Yang file.
     *
     * @param yangFile
     *            yang file to parse
     * @return the schema definition of Yang module defined in .Yang file.
     */
    public Module parseYangModel(final String yangFile);

    /**
     * Parse one or more Yang model files and return the definitions of Yang
     * modules defined in *.Yang files; <br>
     * This method SHOULD be used if user need to parse multiple yang models
     * that are referenced either through import or include statements.
     *
     * @param yangFiles
     *            yang files to parse
     * @return Set of Yang Modules
     */
    public Set<Module> parseYangModels(final String... yangFiles);

    public Set<Module> parseYangModelsFromStreams(
            final InputStream... yangModelStreams);

    /**
     * Creates {@link SchemaContext} from specified Modules. The modules SHOULD
     * not contain any unresolved Schema Nodes or Type Definitions. By
     * unresolved Schema Nodes or Type Definitions we mean that the Module
     * should not contain ANY Schema Nodes that contains
     * {@link UnknownTypeDefinition} and all dependencies although via import or
     * include definitions are resolved.
     *
     * @param modules
     *            Set of Yang Modules
     * @return Schema Context instance constructed from whole Set of Modules.
     */
    public SchemaContext resolveSchemaContext(final Set<Module> modules);
}
