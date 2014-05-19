/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import com.google.common.base.Preconditions;

abstract class AbstractDataTreeCandidate implements DataTreeCandidate {
	private final InstanceIdentifier rootPath;
	private final NodeModification modificationRoot;

	protected AbstractDataTreeCandidate(final InstanceIdentifier rootPath, NodeModification modificationRoot) {
		this.rootPath = Preconditions.checkNotNull(rootPath);
		this.modificationRoot = Preconditions.checkNotNull(modificationRoot);
	}

	@Override
	public final InstanceIdentifier getRootPath() {
		return rootPath;
	}

	@Override
	public final NodeModification getModificationRoot() {
		return modificationRoot;
	}
}
