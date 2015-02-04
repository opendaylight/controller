/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

/**
 * Message sent to a transaction actor to create a snapshot of the data store.
 *
 * @author Thomas Pantelis
 */
public class CreateSnapshot {
    // Note: This class does not need to Serializable as it's only sent locally.

    public static final CreateSnapshot INSTANCE = new CreateSnapshot();
}
