/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import java.io.Serializable;

/**
 * A reply message indicating success.
 *
 * @author Thomas Pantelis
 */
public final class SuccessReply implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final SuccessReply INSTANCE = new SuccessReply();

    private SuccessReply() {
    }
}
