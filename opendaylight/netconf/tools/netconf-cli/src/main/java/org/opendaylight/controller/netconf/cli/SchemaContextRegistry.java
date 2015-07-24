/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli;

import com.google.common.base.Optional;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Contains the local schema context (containing local commands) and remote schema context (remote commands)
 *
 * Remote commands are set only after the connection is fully established. So classes using the remote schema context
 */
public class SchemaContextRegistry {

    private final SchemaContext localSchemaContext;
    private SchemaContext remoteSchemaContext;

    public SchemaContextRegistry(final SchemaContext localSchemaContext) {
        this.localSchemaContext = localSchemaContext;
    }

    public synchronized Optional<SchemaContext> getRemoteSchemaContext() {
        return Optional.fromNullable(remoteSchemaContext);
    }

    public SchemaContext getLocalSchemaContext() {
        return localSchemaContext;
    }

    public synchronized void setRemoteSchemaContext(final SchemaContext remoteSchemaContext) {
        this.remoteSchemaContext = remoteSchemaContext;
    }
}
