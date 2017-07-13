/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider.impl;

import java.util.Collection;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListeningException;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

public class IdIntsDOMDataTreeLIstener extends AbstractDataListener implements DOMDataTreeListener {
     @Override
    public void onDataTreeChanged(@Nonnull final Collection<DataTreeCandidate> changes,
                                  @Nonnull final Map<DOMDataTreeIdentifier, NormalizedNode<?, ?>> subtrees) {
        onReceivedChanges(changes);
    }

    @Override
    public void onDataTreeFailed(@Nonnull final Collection<DOMDataTreeListeningException> causes) {
        onReceivedError(causes);
    }
}
