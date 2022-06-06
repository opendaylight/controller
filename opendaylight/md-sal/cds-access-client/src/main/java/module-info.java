/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
module org.opendaylight.controller.cluster.access.client {
    exports org.opendaylight.controller.cluster.access.client;

    requires transitive org.opendaylight.controller.cluster.access;
    requires transitive org.opendaylight.controller.repackaged.akka;
    requires com.google.common;
    requires org.slf4j;
    requires scala.library;
    
    // Annotations
    requires static org.checkerframework.checker.qual;
    requires static com.github.spotbugs.annotations;
}
