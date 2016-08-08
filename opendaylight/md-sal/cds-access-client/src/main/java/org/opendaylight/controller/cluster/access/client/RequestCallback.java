/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.Response;

@FunctionalInterface
public interface RequestCallback {
    /**
     * Invoked when a particular request completes.
     *
     * @param response Response to the request
     * @return Next client actor behavior
     */
    @Nullable ClientActorBehavior complete(@Nonnull Response<?, ?> response);
}
