/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;

/**
 * Creates {@link DataBrokerSnapshot} instances from the current state content
 * of a DataBroker.
 *
 * @see DataBrokerSnapshot
 *
 * @author Michael Vorburger
 */
public class DataBrokerSnapshoter {

    public DataBrokerSnapshoter(DataBroker dataBroker) {
    }

    public DataBrokerSnapshot snapshot() {
        return null;
    }

    // public void restore(DataBrokerSnapshot snapshot)

    // public void clear()
}
