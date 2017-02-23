/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.util.concurrent.TimeUnit;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * Interface for a class the publishes data tree notifications.
 *
 * @author Thomas Pantelis
 */
interface ShardDataTreeNotificationPublisher {
    long PUBLISH_DELAY_THRESHOLD_IN_MS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);

    void publishChanges(final DataTreeCandidate candidate, String logContext);
}
