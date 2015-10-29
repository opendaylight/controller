/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.protobuff.client.messages;

/**
 * This is a tagging interface for a Payload implementation that needs to always be persisted regardless of
 * whether or not the component is configured to be persistent.
 *
 * @author Thomas Pantelis
 */
public interface PersistentPayload {
}
