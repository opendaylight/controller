/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test;

import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * TODO Doc.
 *
 * @author Michael Vorburger
 */
public final class SchemaContextSingleton {

    private static SchemaContext staticSchemaContext;

    public static synchronized SchemaContext getSchemaContext(Supplier<SchemaContext> supplier) throws Exception {
        if (staticSchemaContext == null) {
            staticSchemaContext = supplier.get();
        }
        return staticSchemaContext;
    }

    private SchemaContextSingleton() { }

    @FunctionalInterface
    public interface Supplier<T> {
        T get() throws Exception;
    }
}
