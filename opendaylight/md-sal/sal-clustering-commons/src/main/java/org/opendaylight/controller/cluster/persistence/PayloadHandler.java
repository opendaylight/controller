/*
 * Copyright (c) 2023 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.persistence;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface PayloadHandler {

    void writeTo(DataOutput out, SerializablePayload payload) throws IOException;

    SerializablePayload readFrom(DataInput in) throws IOException;


}
