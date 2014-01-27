/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl.util

import org.opendaylight.controller.md.sal.common.impl.routing.AbstractDataReadRouter
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.DataObject

class BindingAwareDataReaderRouter extends AbstractDataReadRouter<InstanceIdentifier<? extends DataObject>, DataObject> {
    
    override protected merge(InstanceIdentifier<? extends DataObject> path, Iterable<DataObject> data) {
        return data.iterator.next;
    }
    
}
