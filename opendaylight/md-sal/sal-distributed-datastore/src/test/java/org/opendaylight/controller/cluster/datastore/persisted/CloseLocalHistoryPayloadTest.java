/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

public class CloseLocalHistoryPayloadTest extends AbstractIdentifiablePayloadTest<CloseLocalHistoryPayload> {
    public CloseLocalHistoryPayloadTest() {
        super(CloseLocalHistoryPayload.create(newHistoryId(0), 512), 264);
    }
}
