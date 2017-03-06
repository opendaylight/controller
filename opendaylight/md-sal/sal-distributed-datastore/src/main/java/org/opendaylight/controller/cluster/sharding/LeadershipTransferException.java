/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;

@Beta
public class LeadershipTransferException extends Exception {
    private static final long serialVersionUID = 1L;

    public LeadershipTransferException(final @Nonnull String message) {
        super(message);
    }

    public LeadershipTransferException(final @Nonnull String message, final @Nonnull Throwable cause) {
        super(message, cause);
    }
}
