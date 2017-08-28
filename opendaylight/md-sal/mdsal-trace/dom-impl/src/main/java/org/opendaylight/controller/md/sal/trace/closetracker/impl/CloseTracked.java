/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.closetracker.impl;

import java.time.Instant;
import javax.annotation.Nullable;

/**
 * Object which can track where it has been created, and if it has been correctly "closed".
 *
 * <p>Includes preserving the context of the call stack which created this object, and the instant it was created.
 *
 * @author Michael Vorburger.ch
 */
public interface CloseTracked<T extends CloseTracked<T>> {

    Instant getObjectCreated();

    @Nullable StackTraceElement[] getAllocationContextStackTrace();

}
