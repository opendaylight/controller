/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
module org.opendaylight.controller.cluster.access {
    exports org.opendaylight.controller.cluster.access;
    exports org.opendaylight.controller.cluster.access.commands;
    exports org.opendaylight.controller.cluster.access.concepts;

    requires transitive java.desktop;
    requires transitive org.opendaylight.controller.repackaged.pekko;
    requires transitive org.opendaylight.yangtools.binding.spec;
    requires transitive org.opendaylight.yangtools.yang.data.api;
    requires com.google.common;
    requires org.opendaylight.controller.scala3.library;
    requires org.opendaylight.yangtools.yang.data.codec.binfmt;
    requires org.opendaylight.yangtools.yang.data.impl;
    requires org.slf4j;

    // Annotations
    requires static transitive org.eclipse.jdt.annotation;
    requires static com.github.spotbugs.annotations;
}
