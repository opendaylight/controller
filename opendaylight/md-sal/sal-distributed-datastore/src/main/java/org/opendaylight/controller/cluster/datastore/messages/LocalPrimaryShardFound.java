/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.data.tree.api.ReadOnlyDataTree;

/**
 * Local message sent in reply to FindPrimaryShard to indicate the primary shard is local to the caller.
 *
 * @author Thomas Pantelis
 */
public class LocalPrimaryShardFound {
    private final String primaryPath;
    private final ReadOnlyDataTree localShardDataTree;

    public LocalPrimaryShardFound(final @NonNull String primaryPath,
            final @NonNull ReadOnlyDataTree localShardDataTree) {
        this.primaryPath = requireNonNull(primaryPath);
        this.localShardDataTree = requireNonNull(localShardDataTree);
    }

    public @NonNull String getPrimaryPath() {
        return primaryPath;
    }

    public @NonNull ReadOnlyDataTree getLocalShardDataTree() {
        return localShardDataTree;
    }

    @Override
    public String toString() {
        return "LocalPrimaryShardFound [primaryPath=" + primaryPath
                + ", localShardDataTree=" + Objects.toIdentityString(localShardDataTree) + "]";
    }
}
