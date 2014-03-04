/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mapping.api;

import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.w3c.dom.Document;

/**
 * NetconfOperation handles netconf requests. Multiple operations might be
 * capable of handling one request at the same time. In such case, these
 * operations are chained (ordered by HandlingPriority returned by canHandle
 * method) and executed.
 *
 * Operation can be declared as singleton or last in chain (see abstract
 * implementations in netconf-util). If the operation is not singleton or last,
 * it is responsible for the execution of subsequent operation and for merging
 * the results.
 *
 */
public interface NetconfOperation {

    /**
     * Singleton operations should return
     * HandlingPriority.HANDLE_WITH_MAX_PRIORITY, last operations
     * HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY.
     *
     * @param requestMessage
     * @return
     */
    HandlingPriority canHandle(Document requestMessage);

    /**
     * Execute current netconf operation and trigger execution of subsequent
     * operations. subsequentOperation parameter will provide information, if
     * current operation is the termination point in execution. In case of
     * last/singleton operation, subsequentOperation must indicate termination
     * point.
     *
     * @param requestMessage
     * @param subsequentOperation
     *            execution of subsequent netconf operation
     * @return
     * @throws NetconfDocumentedException
     */
    Document handle(Document requestMessage, NetconfOperationChainedExecution subsequentOperation)
            throws NetconfDocumentedException;
}
