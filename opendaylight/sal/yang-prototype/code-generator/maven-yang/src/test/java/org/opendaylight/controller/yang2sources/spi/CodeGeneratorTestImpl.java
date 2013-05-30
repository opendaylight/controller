/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang2sources.spi;

import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;

import java.io.File;
import java.util.Collection;
import java.util.Set;

public class CodeGeneratorTestImpl implements CodeGenerator {

    @Override
    public Collection<File> generateSources(SchemaContext context,
            File outputBaseDir, Set<Module> currentModuleBuilders, File projectMainDir) {
        // no-op
        return null;
    }

}
