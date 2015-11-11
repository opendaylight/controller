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

public interface CodeStringProvider {
    /**
     * Get the string corresponding to the code or null if the code is unknown
     *
     * @param code
     */
    @Nullable
    String getString(Integer code);

    /**
     * Save the string and it's code so that in future when getCode is invoked the saved code
     * may be returned
     *
     * @param str
     * @param code
     */
    void saveString(@Nonnull String str, Integer code);
}
