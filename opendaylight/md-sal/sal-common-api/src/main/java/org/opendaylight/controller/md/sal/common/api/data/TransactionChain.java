/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

public interface TransactionChain<P/* extends Path<P> */, D> extends AutoCloseable {
	/**
	 * Create a new transaction which will continue the chain. The previous transaction
	 * has to be either COMMITTED or CANCELLED.
	 *
	 * @return New transaction in the chain.
	 * @throws IllegalStateException if the previous transaction was not COMMITTED or CANCELLED.
	 * @throws TransactionChainClosedException if the chain has been closed.
	 */
	DataModification<P, D> newTransaction();

	@Override
	void close();
}

