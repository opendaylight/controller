/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Interface defining a result of an RPC call.
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_INTERFACE", justification = "Migration")
public interface DOMRpcResult extends org.opendaylight.mdsal.dom.api.DOMRpcResult {
}
