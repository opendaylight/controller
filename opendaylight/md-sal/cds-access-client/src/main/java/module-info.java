/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
module org.opendaylight.controller.cluster.access.client {
    exports org.opendaylight.controller.cluster.access.client;

    requires transitive com.google.common;
    requires transitive org.opendaylight.controller.cluster.access.api;
    requires transitive org.opendaylight.controller.cluster.commons;
    requires transitive org.opendaylight.controller.repackaged.pekko;
    requires transitive org.opendaylight.raft.spi;
    requires transitive org.opendaylight.yangtools.concepts;
    requires org.opendaylight.controller.scala3.library;
    requires org.slf4j;

    // Annotations
    requires static transitive org.eclipse.jdt.annotation;
    requires static com.github.spotbugs.annotations;
    requires static org.checkerframework.checker.qual;
    requires static org.osgi.annotation.bundle;
}
