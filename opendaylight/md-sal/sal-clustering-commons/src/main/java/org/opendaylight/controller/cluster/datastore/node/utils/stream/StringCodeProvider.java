/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface StringCodeProvider {
    /**
     * Given a string provide a code that should be used to encode that string when serializing
     *
     * @param str the string to be encoded
     * @return an integer code if it already exists else null
     */
    @Nullable
    Integer getCode(@Nonnull String str);

    /**
     * Generate a unique integer code which will be used to encode a string
     *
     * @return
     */
    Integer createCode(@Nonnull String str);

    /**
     * Check if the StringCodeProvider is compatible with a specific version of the NormalizedNodeOutputStreamWriter
     *
     * @return
     */
    boolean isCompatibleWith(NormalizedNodeOutputStreamWriterVersion version);
}
