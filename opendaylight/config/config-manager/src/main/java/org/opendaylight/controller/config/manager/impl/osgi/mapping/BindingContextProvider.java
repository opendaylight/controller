/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi.mapping;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import org.opendaylight.yangtools.sal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates and initializes {@link BindingRuntimeContext}, which is used to resolve Identity classes from QName.
 * An instance of {@link BindingRuntimeContext} is available only after first schema context was successfully built.
 */
// TODO move to yang runtime
public class BindingContextProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BindingContextProvider.class);

    private BindingRuntimeContext current;

    public synchronized void update(final ClassLoadingStrategy classLoadingStrategy, final SchemaContextProvider ctxProvider) {
        this.current = BindingRuntimeContext.create(classLoadingStrategy, ctxProvider.getSchemaContext());
    }

    public synchronized BindingRuntimeContext getBindingContext() {
        Preconditions.checkState(this.current != null, "Binding context not yet initialized");
        return this.current;
    }

    @Override
    public synchronized void close() throws Exception {
        LOG.warn("BindingContextProvider close called");
        LOG.warn(Arrays.asList(new Throwable().getStackTrace()).toString());
        this.current = null;
    }
}
