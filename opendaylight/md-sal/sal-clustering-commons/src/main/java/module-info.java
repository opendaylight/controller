/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Common clustering utilities.
 */
module org.opendaylight.controller.cluster.common {
    exports org.opendaylight.controller.cluster.common.actor;
    exports org.opendaylight.controller.cluster.datastore.node.utils;
    exports org.opendaylight.controller.cluster.datastore.node.utils.stream;
    exports org.opendaylight.controller.cluster.datastore.node.utils.transformer;
    exports org.opendaylight.controller.cluster.datastore.util;
    exports org.opendaylight.controller.cluster.messaging;
    exports org.opendaylight.controller.cluster.reporting;
    exports org.opendaylight.controller.cluster.schema.provider;
    exports org.opendaylight.controller.cluster.schema.provider.impl;

    requires transitive com.codahale.metrics;
    requires transitive com.google.common;
    requires transitive org.opendaylight.controller.pekko.support;
    requires transitive org.opendaylight.controller.repackaged.pekko;
    requires transitive org.opendaylight.controller.scala3.library;
    requires transitive org.opendaylight.raft.spi;
    requires transitive org.opendaylight.yangtools.yang.data.api;
    requires transitive org.opendaylight.yangtools.yang.data.codec.binfmt;
    requires transitive org.opendaylight.yangtools.yang.data.util;
    requires transitive org.opendaylight.yangtools.yang.repo.api;
    requires transitive org.opendaylight.yangtools.yang.repo.spi;
    requires transitive typesafe.config;
    requires com.codahale.metrics.jmx;
    requires org.opendaylight.yangtools.yang.data.impl;
    requires org.slf4j;

    // Annotations
    requires static transitive org.eclipse.jdt.annotation;
    requires static com.github.spotbugs.annotations;
    requires static javax.inject;
    requires static org.kohsuke.metainf_services;
    requires static org.osgi.annotation.bundle;
    requires static org.osgi.service.component.annotations;
}
