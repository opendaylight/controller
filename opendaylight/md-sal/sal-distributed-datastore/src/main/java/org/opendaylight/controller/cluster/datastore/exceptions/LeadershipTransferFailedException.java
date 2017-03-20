/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.exceptions;

public class LeadershipTransferFailedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public LeadershipTransferFailedException(final String message) {
        super(message);
    }
}
