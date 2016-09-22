/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api.data;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 *
 * @deprecated Replaced by {@link org.opendaylight.controller.md.sal.binding.api.DataChangeListener}
 *
 */
@Deprecated
public interface DataChangeListener extends
        org.opendaylight.controller.md.sal.common.api.data.DataChangeListener<InstanceIdentifier<? extends DataObject>, DataObject> {

    @Override
    void onDataChanged(DataChangeEvent<InstanceIdentifier<?>, DataObject> change);
}
