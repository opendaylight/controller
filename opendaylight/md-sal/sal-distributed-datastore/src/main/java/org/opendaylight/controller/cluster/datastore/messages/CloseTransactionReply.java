/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

@Deprecated(since = "9.0.0", forRemoval = true)
public class CloseTransactionReply extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    public CloseTransactionReply() {
    }
}
