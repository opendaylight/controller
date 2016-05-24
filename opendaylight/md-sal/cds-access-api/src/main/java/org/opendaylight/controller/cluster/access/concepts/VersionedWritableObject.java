/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;
import java.io.IOException;
import java.io.ObjectOutput;

/**
 * Versioned counterpart of the {@link WritableObject} abstraction. Classes implementing this interface guarantee
 * that the data stream will be portable to the next major API version.
 *
 * @author Robert Varga
 */
// FIXME: this really should go into yangtools/common/concepts.
@Beta
public interface VersionedWritableObject {
    /**
     * Serialize this object into a {@link ObjectOutput} as a versioned, evolvable stream.
     *
     * @param out Output
     * @throws IOException if the output fails
     */
    void writeTo(ObjectOutput out) throws IOException;
}
