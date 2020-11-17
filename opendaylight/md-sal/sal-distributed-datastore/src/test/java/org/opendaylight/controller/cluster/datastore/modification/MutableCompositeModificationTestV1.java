/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.modification;

public class MutableCompositeModificationTestV1 extends MutableCompositeModificationTest {

    @Override
    protected MutableCompositeModification getModification() {
        return new MutableCompositeModificationV1();
    }
}
