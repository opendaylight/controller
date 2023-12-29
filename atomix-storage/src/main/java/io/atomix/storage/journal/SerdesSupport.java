/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import java.io.IOException;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public interface SerdesSupport<T> {
    // FIXME: the string here should be:
    //        - restricted to latin-1 (i.e. 0-127 value)
    //        - restricted to 127 characters (and, by extension, bytes)
    //        - consider creating a separate class for that
    String symbolicName();

    Class<T> typeClass();

    T readObject(SerdesDataInput input) throws IOException;

    void writeObject(T obj, SerdesDataOutput output) throws IOException;
}