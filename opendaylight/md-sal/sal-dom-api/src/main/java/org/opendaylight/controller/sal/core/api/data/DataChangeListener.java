/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api.data;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 *
 * @deprecated Replaced by {@link org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener}
 */
@Deprecated
public interface DataChangeListener
        extends
        org.opendaylight.controller.md.sal.common.api.data.DataChangeListener<YangInstanceIdentifier, CompositeNode> {

    @Override
    public void onDataChanged(DataChangeEvent<YangInstanceIdentifier, CompositeNode> change);
}
