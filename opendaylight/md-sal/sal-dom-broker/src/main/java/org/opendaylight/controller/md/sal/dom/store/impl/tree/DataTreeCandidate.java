/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.data.NodeModification;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.data.StoreMetadataNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public interface DataTreeCandidate extends AutoCloseable {
	@Override
	void close();

	InstanceIdentifier getRootPath();

	@Deprecated
	NodeModification getModificationRoot();

	@Deprecated
	StoreMetadataNode getBeforeRoot();

	@Deprecated
	StoreMetadataNode getAfterRoot();
}
