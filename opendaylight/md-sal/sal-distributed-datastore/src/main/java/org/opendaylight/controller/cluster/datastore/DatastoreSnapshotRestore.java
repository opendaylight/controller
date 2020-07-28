/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.annotations.Beta;
import java.util.Optional;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;

/**
 * This class looks for a previously saved data store backup file in a directory and, if found, de-serializes
 * the DatastoreSnapshot instances. This class has a static singleton that is created on bundle activation.
 *
 * @author Thomas Pantelis
 */
@Beta
public interface DatastoreSnapshotRestore {

    Optional<DatastoreSnapshot> getAndRemove(String datastoreType);
}
