/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import java.util.Collection;
import java.util.EventListener;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * Interface implemented by data consumers, e.g. processes wanting to act on data
 * after it has been introduced to the conceptual data tree.
 */
public interface DOMDataTreeListener extends EventListener {
    /**
     * Invoked whenever one or more registered subtrees change. The logical changes are reported,
     * as well as the roll up of new state for all subscribed subtrees.
     *
     * @param changes The set of changes being reported. Each subscribed subtree may be present
     *                at most once.
     * @param subtrees Per-subtree state as visible after the reported changes have been applied.
     *                 This includes all the subtrees this listener is subscribed to, even those
     *                 which have not changed.
     */
    void onDataTreeChanged(@Nonnull Collection<DataTreeCandidate> changes, @Nonnull Map<DOMDataTreeIdentifier, NormalizedNode<?, ?>> subtrees);
}
