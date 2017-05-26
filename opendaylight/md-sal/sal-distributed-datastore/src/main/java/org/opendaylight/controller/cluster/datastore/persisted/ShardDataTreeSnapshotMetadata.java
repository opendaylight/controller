/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.base.Verify;
import java.io.Externalizable;
import java.io.Serializable;
import javax.annotation.Nonnull;

/**
 * Base class for various bits of metadata attached to a {@link MetadataShardDataTreeSnapshot}. This class is not
 * an interface because we want to make sure all subclasses implement the externalizable proxy pattern, for which
 * we need to force {@link #writeReplace()} to be abstract. We do that by making it final and exposing a protected
 * {@link #externalizableProxy()} method.
 *
 * <p>
 * All concrete subclasses of this class should be final so as to form a distinct set of possible metadata. Since
 * metadata is serialized along with {@link MetadataShardDataTreeSnapshot}, this set is part of the serialization format
 * guarded by {@link PayloadVersion}.
 *
 * <p>
 * If a new metadata type is introduced or a type is removed, {@link PayloadVersion} needs to be bumped to ensure
 * compatibility.
 *
 * @author Robert Varga
 */
public abstract class ShardDataTreeSnapshotMetadata<T extends ShardDataTreeSnapshotMetadata<T>>
        implements Serializable {
    private static final long serialVersionUID = 1L;

    ShardDataTreeSnapshotMetadata() {
        // Prevent subclassing from outside of this package
    }

    final Object writeReplace() {
        return Verify.verifyNotNull(externalizableProxy(), "Null externalizable proxy from %s", getClass());
    }

    /**
     * Return an Externalizable proxy.
     *
     * @return Externalizable proxy, may not be null
     */
    protected abstract @Nonnull Externalizable externalizableProxy();

    public abstract Class<T> getType();
}
