/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.benchmark.sharding.impl.tests;

import java.util.Collection;
import java.util.Map;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListeningException;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A DataTreeChangeListener for validation of data pushed into the data
 *  store during the shard test.
 * @author jmedved
 *
 */
public class ValidationListener implements DOMDataTreeListener {
    private static final Logger LOG = LoggerFactory.getLogger(RoundRobinShardTest.class);

    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> collection,
            final Map<DOMDataTreeIdentifier, NormalizedNode<?, ?>> map) {
        LOG.warn("Received onDataTreeChanged {}, data: {}", collection, map);
    }

    @Override
    public void onDataTreeFailed(final Collection<DOMDataTreeListeningException> collection) {
        LOG.error("Received onDataTreeFailed {}", collection);
    }

}
