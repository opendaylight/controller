/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test;

import org.junit.Before;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public abstract class AbstractSchemaAwareTest  {

    protected Iterable<YangModuleInfo> getModuleInfos() throws Exception {
        return BindingReflections.loadModuleInfos();
    }

    protected SchemaContext getSchemaContext() throws Exception {
        final Iterable<YangModuleInfo> moduleInfos = getModuleInfos();
        final ModuleInfoBackedContext moduleContext = ModuleInfoBackedContext.create();
        moduleContext.addModuleInfos(moduleInfos);
        return moduleContext.tryToCreateSchemaContext().get();
    }

    @Before
    public final void setup() throws Exception {
        setupWithSchema(getSchemaContext());
    }

    /**
     * Setups test with Schema context.
     * This method is called before {@link #setupWithSchemaService(SchemaService)}
     */
    protected abstract void setupWithSchema(SchemaContext context);

}
