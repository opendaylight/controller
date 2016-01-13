/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import java.io.Serializable;

/**
 * Message sent to query another node's last applied log index.
 *
 * @author Thomas Pantelis
 */
public class GetLastAppliedIndex implements Serializable {
    private static final long serialVersionUID = 1L;
}
