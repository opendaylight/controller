/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public interface DOMStoreReadTransaction extends DOMStoreTransaction {

    /**
     * Reads data from provided logical data store located at provided path
     *
     * @param path
     *            Path which uniquely identifies subtree which client want to
     *            read
     * @return a CheckFuture containing the result of the read. The Future blocks until the
     *         commit operation is complete. Once complete:
     *         <ul>
     *         <li>If the data at the supplied path exists, the Future returns an Optional object
     *         containing the data.</li>
     *         <li>If the data at the supplied path does not exist, the Future returns
     *         Optional#absent().</li>
     *         <li>If the read of the data fails, the Future will fail with a
     *         {@link ReadFailedException} or an exception derived from ReadFailedException.</li>
     *         </ul>
     */
    CheckedFuture<Optional<NormalizedNode<?,?>>, ReadFailedException> read(YangInstanceIdentifier path);

    /**
     * Checks if data is available in the logical data store located at provided path.
     * <p>
     *
     * Note: a successful result from this method makes no guarantee that a subsequent call to {@link #read}
     * will succeed. It is possible that the data resides in a data store on a remote node and, if that
     * node goes down or a network failure occurs, a subsequent read would fail. Another scenario is if
     * the data is deleted in between the calls to <code>exists</code> and <code>read</code>
     *
     * @param path
     *            Path which uniquely identifies subtree which client want to
     *            check existence of
     * @return a CheckFuture containing the result of the check.
     *         <ul>
     *         <li>If the data at the supplied path exists, the Future returns a Boolean
     *         whose value is true, false otherwise</li>
     *         <li>If checking for the data fails, the Future will fail with a
     *         {@link ReadFailedException} or an exception derived from ReadFailedException.</li>
     *         </ul>
     */
    CheckedFuture<Boolean, ReadFailedException> exists(YangInstanceIdentifier path);
}
