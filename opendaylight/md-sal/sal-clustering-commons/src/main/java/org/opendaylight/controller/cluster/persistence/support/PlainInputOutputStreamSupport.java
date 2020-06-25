/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.persistence.support;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.jdt.annotation.NonNull;

final class PlainInputOutputStreamSupport extends InputOutputStreamFactory {
    static final @NonNull PlainInputOutputStreamSupport INSTANCE = new PlainInputOutputStreamSupport();

    private PlainInputOutputStreamSupport() {
        // Hidden on purpose
    }

    @Override
    InputStream createInputStream(final File file) throws IOException {
        return defaultCreateInputStream(file);
    }

    @Override
    OutputStream createOutputStream(final File file) throws IOException {
        return defaultCreateOutputStream(file);
    }
}
