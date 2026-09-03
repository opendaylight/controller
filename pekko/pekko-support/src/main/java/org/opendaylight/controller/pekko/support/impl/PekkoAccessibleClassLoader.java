/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.impl;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.pekko.support.spi.PekkoAccessibleClasses;
import org.opendaylight.yangtools.concepts.AccessControllerCompat;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ClassLoader} which delegates to a set of {@link PekkoAccessibleClass}.
 */
@Component(service = PekkoAccessibleClassLoader.class)
public final class PekkoAccessibleClassLoader extends ClassLoader {
    private static final Logger LOG = LoggerFactory.getLogger(PekkoAccessibleClassLoader.class);

    @NonNullByDefault
    private final List<ClassLoader> loaders;

    @NonNullByDefault
    private PekkoAccessibleClassLoader(final Stream<PekkoAccessibleClasses> accessibleClasses) {
        super(UUID.randomUUID().toString(), PekkoAccessibleClassLoader.class.getClassLoader());
        loaders = List.of(accessibleClasses.distinct()
            .map(PekkoAccessibleClasses::asClassLoader)
            .distinct()
            .toArray(ClassLoader[]::new));
    }

    @Activate
    @NonNullByDefault
    public PekkoAccessibleClassLoader(
            final @Reference(
                cardinality = ReferenceCardinality.AT_LEAST_ONE,
                policyOption = ReferencePolicyOption.GREEDY) List<PekkoAccessibleClasses> accessibleClasses) {
        this(accessibleClasses.stream());
        LOG.info("PekkoAccessibleClassLoader {} started", getName());
    }

    @NonNullByDefault
    static PekkoAccessibleClassLoader of(final Stream<PekkoAccessibleClasses> accessibleClasses) {
        return AccessControllerCompat.get(() -> new PekkoAccessibleClassLoader(accessibleClasses));
    }

    @Deactivate
    void deactivate() {
        LOG.info("PekkoAccessibleClassLoader {} stopped", getName());
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        ClassNotFoundException cause = null;
        for (var loader : loaders) {
            try {
                return loader.loadClass(name);
            } catch (ClassNotFoundException e) {
                if (cause == null) {
                    cause = e;
                } else {
                    cause.addSuppressed(e);
                }
            }
        }
        throw cause != null ? cause : new ClassNotFoundException(name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("uuid", getName()).add("loaders", loaders).toString();
    }
}
