/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A set of classes that are required to be accessible to {@link ActorContextInstance}'s invocations of
 * {@link Class#forName(String)}.
 *
 * @since 14.0.0
 */
@NonNullByDefault
public interface PekkoAccessibleClasses {
    /**
     * {@return a ClassLoader capable of loading these classes}
     */
    ClassLoader asClassLoader();
}
