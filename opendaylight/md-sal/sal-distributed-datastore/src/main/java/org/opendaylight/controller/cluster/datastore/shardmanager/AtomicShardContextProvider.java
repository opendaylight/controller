/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import com.google.common.base.Verify;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;

final class AtomicShardContextProvider extends AtomicReference<SchemaContext> implements SchemaContextProvider {
    private static final long serialVersionUID = 1L;

    @Override
    public SchemaContext getSchemaContext() {
        return Verify.verifyNotNull(get());
    }
}