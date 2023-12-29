/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableBiMap;

/**
 * Manifest of current encoding used at a place in the Journal.
 * Each JournalSegment has the manifest in its leading frames.
 */
record SerdesManifest(ImmutableBiMap<Integer, String> objIdToSymbolicName) {
    // The idea here is simple: we have a BiMap<Integer, String>, where the string is a unique name.
    // Keys should be contiguous, so that they really represent a String[] with unique items.
    SerdesManifest {
        requireNonNull(objIdToSymbolicName);
    }
}
