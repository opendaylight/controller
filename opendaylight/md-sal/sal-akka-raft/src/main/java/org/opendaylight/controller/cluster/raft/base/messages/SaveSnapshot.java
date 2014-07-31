/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

import java.io.Serializable;

/**
 * This message is sent by a RaftActor to itself so that a subclass can process
 * it and use it to save it's state
 */
public class SaveSnapshot implements Serializable {
}
