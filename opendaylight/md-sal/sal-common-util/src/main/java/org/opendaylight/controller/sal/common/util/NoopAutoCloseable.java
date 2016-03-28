/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.common.util;

/**
 * An AutoCloseable that does nothing.
 *
 * @author Thomas Pantelis
 */
public final class NoopAutoCloseable implements AutoCloseable {
    public static final NoopAutoCloseable INSTANCE = new NoopAutoCloseable();

    private NoopAutoCloseable() {
    }

    @Override
    public void close() {
    }
}
