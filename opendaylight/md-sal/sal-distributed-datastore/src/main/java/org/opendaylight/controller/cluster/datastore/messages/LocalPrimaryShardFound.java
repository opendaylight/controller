/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.ObjectUtils;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

/**
 * Local message sent in reply to FindPrimaryShard to indicate the primary shard is local to the caller.
 *
 * @author Thomas Pantelis
 */
public class LocalPrimaryShardFound {

    private final String primaryPath;
    private final DataTree localShardDataTree;

    public LocalPrimaryShardFound(@Nonnull String primaryPath, @Nonnull DataTree localShardDataTree) {
        this.primaryPath = Preconditions.checkNotNull(primaryPath);
        this.localShardDataTree = Preconditions.checkNotNull(localShardDataTree);
    }

    public @Nonnull String getPrimaryPath() {
        return primaryPath;
    }

    public @Nonnull DataTree getLocalShardDataTree() {
        return localShardDataTree;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LocalPrimaryShardFound [primaryPath=").append(primaryPath).append(", localShardDataTree=")
                .append(ObjectUtils.identityToString(localShardDataTree)).append("]");
        return builder.toString();
    }
}
