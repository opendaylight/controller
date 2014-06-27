/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncReadOnlyTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * A transaction that provides a stateful read-only view of the data tree.
 * <p>
 * For more information on usage and examples, please see the documentation in
 *  {@link org.opendaylight.controller.md.sal.common.api.data.AsyncReadTransaction}.
 */
public interface ReadOnlyTransaction extends ReadTransaction, AsyncReadOnlyTransaction<InstanceIdentifier<?>, DataObject> {

}
