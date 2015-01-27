/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import java.util.EventListener;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;

public interface DOMDataListener extends EventListener {

    /**
     *
     * Invoked when there was data change for the supplied path, which was used
     * to register this listener.
     *
     * <p>
     * This method may be also invoked during registration of the listener if
     * there is any preexisting data in the conceptual data tree for supplied
     * path. This initial event will contain all preexisting data as created.
     *
     * <p>
     * Provided change event MAY NOT provide access to
     * {@link DataTreeCandidateNode#getDataBefore()} or
     * {@link DataTreeCandidateNode#getDataAfter()} for parent nodes specified
     * in registered path.
     *
     * <p>
     * Data Change event MAY NOT be compressed by publisher, this means if it
     * possible to receive {@link DataTreeCandidateNode}, with
     * {@code node.getDataBefore().equals(node.getDataAfter()) == true} even if
     * {@link DataTreeCandidateNode#getModificationType()} is different from
     * {@link ModificationType#UNMODIFIED}.
     *
     * @param change
     *            Data Change Event being delivered.
     */
    void onDataChange(DataTreeCandidate change);

}
