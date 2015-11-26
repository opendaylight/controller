/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Abstract base class for stream dictionaries. This class is kept package-private and we expose only
 * direction-specific subclasses are exposed to outside users to prevent accidental misuse.
 */
@NotThreadSafe
abstract class AbstractStreamDictionary {
    private Object owner;

    AbstractStreamDictionary() {
        // Hidden to prevent instantiation
    }

    final void attach(@Nonnull final Object owner) {
        Preconditions.checkState(this.owner == null, "Dictionary %s is attached to %s, cannot attach to %s", this,
                this.owner, owner);
        this.owner = Preconditions.checkNotNull(owner);
    }

    final void detach(@Nonnull final Object owner) {
        Preconditions.checkState(this.owner != null, "Dictionary %s is already detached", this);
        Preconditions.checkArgument(this.owner.equals(owner), "Dictionary %s is attached to %, not %s", this,
            this.owner, owner);
        this.owner = null;
    }
}
