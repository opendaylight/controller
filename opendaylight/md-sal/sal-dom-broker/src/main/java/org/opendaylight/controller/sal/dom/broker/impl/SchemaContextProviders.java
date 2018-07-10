/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.impl;

import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;

public final class SchemaContextProviders {

    private SchemaContextProviders() {
        throw new UnsupportedOperationException("Utility class.");
    }

    public static SchemaContextProvider fromSchemaService(final DOMSchemaService schemaService) {
        if (schemaService instanceof SchemaContextProvider) {
            return (SchemaContextProvider) schemaService;
        }
        return new SchemaServiceAdapter(schemaService);
    }

    private static final class SchemaServiceAdapter implements SchemaContextProvider, Delegator<DOMSchemaService> {

        private final DOMSchemaService service;

        SchemaServiceAdapter(final DOMSchemaService service) {
            this.service = service;
        }

        @Override
        public SchemaContext getSchemaContext() {
            return service.getGlobalContext();
        }

        @Override
        public DOMSchemaService getDelegate() {
            return service;
        }

        @Override
        public String toString() {
            return "SchemaServiceAdapter [service=" + service + "]";
        }
    }
}
