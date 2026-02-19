/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import static com.google.common.base.Verify.verifyNotNull;

import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

final class AtomicShardContextProvider extends AtomicReference<EffectiveModelContext> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @NonNull EffectiveModelContext modelContext() {
        return verifyNotNull(get());
    }
}
