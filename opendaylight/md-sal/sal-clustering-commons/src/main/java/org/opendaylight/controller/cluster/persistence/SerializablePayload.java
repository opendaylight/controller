/*
 * Copyright (c) 2023 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.persistence;

import org.opendaylight.controller.cluster.persistence.PayloadRegistry.PayloadTypeCommon;

public interface SerializablePayload {
    PayloadTypeCommon getPayloadType();

    /**
     * Return the estimate of in-memory size of this payload.
     * TODO: rework according to the new serialization
     *
     * @return An estimate of the in-memory size of this payload.
     */
    int size();

    /**
     * Return the estimate of serialized size of this payload when passed through serialization. The estimate needs to
     * be reasonably accurate and should err on the side of caution and report a slightly-higher size in face of
     * uncertainty.
     * TODO: rework according to the new serialization
     *
     * @return An estimate of serialized size.
     */
    int serializedSize();

}
