/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;

/**
 * Factory for creating DatastoreContextIntrospector instances.
 *
 * @author Thomas Pantelis
 */
public interface DatastoreContextIntrospectorFactory {
    @NonNull DatastoreContextIntrospector newInstance(LogicalDatastoreType datastoreType);
}
