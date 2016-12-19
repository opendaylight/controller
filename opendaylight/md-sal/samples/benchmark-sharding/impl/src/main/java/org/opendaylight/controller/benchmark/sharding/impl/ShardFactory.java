/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.benchmark.sharding.impl;

import org.opendaylight.controller.cluster.sharding.DOMDataTreeShardCreationFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.yangtools.concepts.Registration;

public interface ShardFactory {

    ShardRegistration createShard(DOMDataTreeIdentifier prefix)
            throws DOMDataTreeShardingConflictException, DOMDataTreeProducerException,
            DOMDataTreeShardCreationFailedException;

    interface ShardRegistration extends Registration {}

}
