/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Pekko-based RAFT implementation.
 */
module org.opendaylight.controller.cluster.raft {
    exports org.opendaylight.controller.cluster.notifications;
    exports org.opendaylight.controller.cluster.raft;
    exports org.opendaylight.controller.cluster.raft.base.messages;
    exports org.opendaylight.controller.cluster.raft.behaviors;
    exports org.opendaylight.controller.cluster.raft.client.messages;
    exports org.opendaylight.controller.cluster.raft.messages;
    exports org.opendaylight.controller.cluster.raft.persisted;
    exports org.opendaylight.controller.cluster.raft.spi;

    // Normally only end users would needs to expose their actors to Pekko for Props-based instantiation.
    // Our test suite needs this as well.
    opens org.opendaylight.controller.cluster.raft to org.opendaylight.controller.repackaged.pekko;

    requires transitive org.opendaylight.raft.api;
    requires transitive org.opendaylight.raft.spi;
    requires transitive org.opendaylight.yangtools.concepts;
    requires transitive org.opendaylight.controller.repackaged.pekko;
    requires transitive org.opendaylight.controller.cluster.commons;
    requires transitive org.opendaylight.controller.cluster.mgmt.api;
    requires com.google.common;
    requires io.netty.buffer;
    requires org.apache.commons.lang3;
    requires org.opendaylight.controller.scala3.library;
    requires org.opendaylight.raft.journal;
    requires org.opendaylight.yangtools.util;
    requires org.slf4j;

    // Constants only
    requires static org.osgi.framework;

    // Annotations
    requires static transitive org.eclipse.jdt.annotation;
    requires static com.github.spotbugs.annotations;
    requires static org.osgi.annotation.bundle;
}
