/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
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
    requires transitive org.opendaylight.yangtools.yang.data.api;
    requires transitive org.opendaylight.yangtools.yang.binding;
    requires transitive org.opendaylight.controller.repackaged.akka;
    requires com.google.common;
    requires org.opendaylight.yangtools.yang.data.codec.binfmt;
    requires org.opendaylight.yangtools.yang.data.impl; 
    
    // Annotations
    requires static transitive org.eclipse.jdt.annotation;
    requires static com.github.spotbugs.annotations;
}
