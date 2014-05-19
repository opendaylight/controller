/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

final class NoopDataTreeCandidate extends AbstractDataTreeCandidate {
	protected NoopDataTreeCandidate(final InstanceIdentifier rootPath, final NodeModification modificationRoot) {
		super(rootPath, modificationRoot);
	}

	@Override
	public void close() {
		// NO-OP
	}

	@Override
	public StoreMetadataNode getBeforeRoot() {
		return null;
	}

	@Override
	public StoreMetadataNode getAfterRoot() {
		return null;
	}
}
