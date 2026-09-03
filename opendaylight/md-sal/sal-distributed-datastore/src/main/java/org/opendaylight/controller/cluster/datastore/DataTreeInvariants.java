/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeFactory;
import org.opendaylight.yangtools.yang.data.tree.api.TreeType;

/**
 * An invariant capturing infomation needed to instnatiate a {@link DataTree}.
 *
 * @since 14.0.0
 */
@Beta
@NonNullByDefault
public record DataTreeInvariants(DataTreeFactory factory, DataTreeConfiguration configuration) {
    public DataTreeInvariants {
        requireNonNull(factory);
        requireNonNull(configuration);
    }

    public static DataTreeInvariants ofDefault(final DataTreeFactory factory, final TreeType type) {
        return new DataTreeInvariants(factory, DataTreeConfiguration.getDefault(type));
    }

    public static DataTreeInvariants ofDefault(final DataTreeFactory factory, final LogicalDatastoreType type) {
        return ofDefault(factory, switch (type) {
            case CONFIGURATION -> TreeType.CONFIGURATION;
            case OPERATIONAL -> TreeType.OPERATIONAL;
        });
    }

    public LogicalDatastoreType type() {
        return switch (configuration.getTreeType()) {
            case CONFIGURATION -> LogicalDatastoreType.CONFIGURATION;
            case OPERATIONAL -> LogicalDatastoreType.OPERATIONAL;
        };
    }

    public YangInstanceIdentifier rootPath() {
        return configuration.getRootPath();
    }
}
