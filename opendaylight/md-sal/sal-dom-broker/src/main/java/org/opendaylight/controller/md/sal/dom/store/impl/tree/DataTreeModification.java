/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Class encapsulation of set of modifications to a base tree. This tree is backed
 * by a read-only snapshot and tracks modifications on top of that. The modification
 * has the ability to rebase itself to a new snapshot.
 */
public interface DataTreeModification extends DataTreeSnapshot {
	void delete(InstanceIdentifier path);
	void merge(InstanceIdentifier path, NormalizedNode<?, ?> data);
	void write(InstanceIdentifier path, NormalizedNode<?, ?> data);
	void seal();
}
