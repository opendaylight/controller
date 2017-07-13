/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider.impl;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

public class IdIntsListener extends AbstractDataListener implements ClusteredDOMDataTreeChangeListener {
    @Override
    public void onDataTreeChanged(@Nonnull final Collection<DataTreeCandidate> changes) {
        onReceivedChanges(changes);
    }
}
