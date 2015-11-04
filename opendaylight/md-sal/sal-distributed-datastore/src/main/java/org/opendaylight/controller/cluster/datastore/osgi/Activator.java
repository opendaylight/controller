/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.osgi;

import org.opendaylight.controller.cluster.datastore.DatastoreSnapshotRestore;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator for the bundle.
 *
 * @author Thomas Pantelis
 */
public class Activator implements BundleActivator {
    private static final String RESTORE_DIRECTORY_PATH = "./clustered-datastore-restore";

    @Override
    public void start(BundleContext context) {
        DatastoreSnapshotRestore.createInstance(RESTORE_DIRECTORY_PATH);
    }

    @Override
    public void stop(BundleContext context) {
        DatastoreSnapshotRestore.removeInstance();
    }
}
