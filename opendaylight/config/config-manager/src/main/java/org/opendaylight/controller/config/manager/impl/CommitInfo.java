/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.controller.config.api.ModuleIdentifier;

/**
 * Structure obtained during first phase commit, contains destroyed modules from
 * previous transactions that need to be closed and all committed modules with
 * meta data.
 */
@Immutable
public class CommitInfo {
    private final List<DestroyedModule> destroyedFromPreviousTransactions;
    private final Map<ModuleIdentifier, ModuleInternalTransactionalInfo> commitMap;

    public CommitInfo(List<DestroyedModule> destroyedFromPreviousTransactions,
            Map<ModuleIdentifier, ModuleInternalTransactionalInfo> commitMap) {
        this.destroyedFromPreviousTransactions = Collections
                .unmodifiableList(destroyedFromPreviousTransactions);
        this.commitMap = Collections.unmodifiableMap(commitMap);
    }

    /**
     * Get ordered list of modules that can be closed in the same order, i.e.
     * first element will be a leaf on which no other module depends, n-th
     * element can only have dependencies with index smaller than n.
     */
    public List<DestroyedModule> getDestroyedFromPreviousTransactions() {
        return destroyedFromPreviousTransactions;
    }

    public Map<ModuleIdentifier, ModuleInternalTransactionalInfo> getCommitted() {
        return commitMap;
    }
}
