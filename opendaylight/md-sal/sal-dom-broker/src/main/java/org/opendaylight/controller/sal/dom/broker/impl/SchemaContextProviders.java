/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.impl;

import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class SchemaContextProviders {

    public static final SchemaContextProvider fromSchemaService(SchemaService schemaService) {
        if (schemaService instanceof SchemaContextProvider) {
            return (SchemaContextProvider) schemaService;
        }
        return new SchemaServiceAdapter(schemaService);
    }

    private final static class SchemaServiceAdapter implements SchemaContextProvider, Delegator<SchemaService> {

        private final SchemaService service;

        public SchemaServiceAdapter(SchemaService service) {
            super();
            this.service = service;
        }

        @Override
        public SchemaContext getSchemaContext() {
            return service.getGlobalContext();
        }

        @Override
        public SchemaService getDelegate() {
            return service;
        }

        @Override
        public String toString() {
            return "SchemaServiceAdapter [service=" + service + "]";
        }
    }
}
