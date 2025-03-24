/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
module org.opendaylight.controller.cluster.mgmt.api {
    exports org.opendaylight.controller.cluster.mgmt.api;
    // FIXME: 12.0.0: collapse this to something more reasonable, like 'raft.server', 'raft.datastore' or somesuch
    exports org.opendaylight.controller.cluster.datastore.jmx.mbeans;
    exports org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

    // Annotation-only dependencies
    requires static transitive java.management;
    requires static transitive org.eclipse.jdt.annotation;
    requires static transitive org.opendaylight.raft.api;
}
