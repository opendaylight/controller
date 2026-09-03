/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Pekko instantiation support.
 */
module org.opendaylight.controller.pekko.support {
    exports org.opendaylight.controller.pekko.support;
    exports org.opendaylight.controller.pekko.support.dagger;
    exports org.opendaylight.controller.pekko.support.osgi;
    exports org.opendaylight.controller.pekko.support.spi;
    opens org.opendaylight.controller.pekko.support.actor to org.opendaylight.controller.repackaged.pekko;

    provides org.opendaylight.controller.pekko.support.dagger.ActorSystemCreator
        with org.opendaylight.controller.pekko.support.impl.DefaultActorSystemCreator;

    requires transitive com.google.common;
    requires transitive org.opendaylight.controller.repackaged.pekko;
    requires transitive org.opendaylight.controller.scala3.library;
    requires transitive typesafe.config;
    requires org.opendaylight.yangtools.concepts;
    requires org.slf4j;

    // Optional OSGi integration
    requires static org.osgi.framework;

    // Annotations
    requires static transitive org.eclipse.jdt.annotation;
    requires static com.github.spotbugs.annotations;
    requires static dagger;
    requires static jakarta.inject;
    requires static javax.inject;
    requires static org.kohsuke.metainf_services;
    requires static org.osgi.annotation.bundle;
    requires static org.osgi.service.component;
    requires static org.osgi.service.component.annotations;
}
