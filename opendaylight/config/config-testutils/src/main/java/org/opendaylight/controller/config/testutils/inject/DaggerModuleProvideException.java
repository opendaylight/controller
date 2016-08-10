/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.testutils.inject;

import dagger.Module;
import dagger.Provides;

/**
 * Exception to throw from Dagger's {@link Module} {@link Provides} methods,
 * which "may only throw unchecked exceptions", when you have to catch checked
 * exceptions while creating objects using code which already throws checked
 * exceptions.
 *
 * <p>
 * When you use this Exception in a Dagger {@link Module}, you should write
 * a simple test for the Module, just to verify it (alone) works at run-time
 * (if there is a checked exception to catch, it probably initializes something
 * that is non-trivial and could fail; so best to have a non-regression test for
 * that Module).
 *
 * <p>
 * Intentionally only has a constructor with root cause, and none with only a
 * message (as this exception is specifically to re-throw another checked
 * exception as unchecked, only), and none with message and root cause (because
 * there typically isn't much of any useful additional custom message to provide
 * from within a failing {@link Module} {@link Provides} method).
 *
 * @author Michael Vorburger
 */
public class DaggerModuleProvideException extends RuntimeException {

    private static final long serialVersionUID = 8503927362559575492L;

    public DaggerModuleProvideException(Exception cause) {
        super(cause.getMessage(), cause);
    }

}
