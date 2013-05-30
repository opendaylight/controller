/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.api;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
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
     * Parse one or more Yang model files and return the definitions of Yang
     * modules defined in *.Yang files; <br>
     * This method SHOULD be used if user need to parse multiple yang models
     * that are referenced either through import or include statements.
     *
     * @param yangFiles
     *            yang files to parse
     * @return Set of Yang Modules
     */
    Set<Module> parseYangModels(final List<File> yangFiles);

    /**
     * Equivalent to {@link #parseYangModels(List)} that returns parsed modules
     * mapped to Files from which they were parsed.
     *
     * @param yangFiles
     *            yang files to parse
     * @return Map of Yang Modules
     */
    Map<File, Module> parseYangModelsMapped(final List<File> yangFiles);

    Set<Module> parseYangModelsFromStreams(
            final List<InputStream> yangModelStreams);

    Map<InputStream, Module> parseYangModelsFromStreamsMapped(
            final List<InputStream> yangModelStreams);

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
    SchemaContext resolveSchemaContext(final Set<Module> modules);
}
