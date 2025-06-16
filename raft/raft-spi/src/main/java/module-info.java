/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * RAFT APIs.
 */
module org.opendaylight.raft.spi {
    exports org.opendaylight.raft.spi;

    requires transitive io.netty.buffer;
    requires transitive org.opendaylight.raft.api;
    requires com.google.common;
    requires org.lz4.java;
    requires org.slf4j;

    // Annotations
    requires static transitive org.eclipse.jdt.annotation;
    requires static org.checkerframework.checker.qual;
    requires static org.osgi.annotation.bundle;
}
