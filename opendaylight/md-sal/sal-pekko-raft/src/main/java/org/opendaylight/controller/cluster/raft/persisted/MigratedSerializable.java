/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Transitional marker interface for use with evolution of serializable classes held in persistence. This interface
 * should be implemented by replacement classes for as long as the old classes are supported.
 *
 * @author Robert Varga
 */
public interface MigratedSerializable extends Serializable {
    /**
     * Return true if this object was created from a previous serialization format.
     *
     * @return true if this object was created from a previous serialization format, false otherwise.
     */
    boolean isMigrated();

    /**
     * Return a serialization proxy. Implementations should not return this object, but rather use Externalizable
     * proxy pattern so their serialization format is not tied to this interface.
     *
     * @return Serialization proxy.
     */
    @NonNull Object writeReplace();
}
