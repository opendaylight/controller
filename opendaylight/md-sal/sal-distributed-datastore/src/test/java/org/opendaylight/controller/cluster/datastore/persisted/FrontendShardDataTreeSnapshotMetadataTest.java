/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.persisted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.primitives.UnsignedLong;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

public class FrontendShardDataTreeSnapshotMetadataTest {

    @Test(expected = NullPointerException.class)
    public final void testCreateMetadataSnapshotNullInput() {
        new FrontendShardDataTreeSnapshotMetadata(null);
    }

    @Test
    public final void testCreateMetadataSnapshotEmptyInput() throws Exception {
        final FrontendShardDataTreeSnapshotMetadata emptySnapshot = createEmptyMetadataSnapshot();
        final FrontendShardDataTreeSnapshotMetadata emptyCopySnapshot = copy(emptySnapshot);
        final FrontendShardDataTreeSnapshotMetadata differentSnapshot = createMetadataSnapshot(1);
        testEquals(emptySnapshot, emptyCopySnapshot, differentSnapshot);
        testHashCode(emptySnapshot, emptyCopySnapshot);
        testSerialization(emptySnapshot, emptyCopySnapshot, differentSnapshot);
    }

    @Test
    public final void testSerializeEmptyMetadataSnapshot() throws Exception {
        final FrontendShardDataTreeSnapshotMetadata emptySnapshot = createEmptyMetadataSnapshot();
        final FrontendShardDataTreeSnapshotMetadata equalEmptySnapshot = createEmptyMetadataSnapshot();
        final FrontendShardDataTreeSnapshotMetadata differentSnapshot = createMetadataSnapshot(1);
        testEquals(emptySnapshot, equalEmptySnapshot, differentSnapshot);
        testHashCode(emptySnapshot, equalEmptySnapshot);
        testSerialization(emptySnapshot, equalEmptySnapshot, differentSnapshot);
    }

    @Test
    public final void testSerializeMetadataSnapshotWithOneClient() throws Exception {
        final FrontendShardDataTreeSnapshotMetadata emptySnapshot = createMetadataSnapshot(1);
        final FrontendShardDataTreeSnapshotMetadata equalEmptySnapshot = createMetadataSnapshot(1);
        final FrontendShardDataTreeSnapshotMetadata differentSnapshot = createMetadataSnapshot(2);
        testEquals(emptySnapshot, equalEmptySnapshot, differentSnapshot);
        testHashCode(emptySnapshot, equalEmptySnapshot);
        testSerialization(emptySnapshot, equalEmptySnapshot, differentSnapshot);
    }

    @Test
    public final void testSerializeMetadataSnapshotWithMoreClients() throws Exception {
        final FrontendShardDataTreeSnapshotMetadata emptySnapshot = createMetadataSnapshot(2);
        final FrontendShardDataTreeSnapshotMetadata equalEmptySnapshot = createMetadataSnapshot(2);
        final FrontendShardDataTreeSnapshotMetadata differentSnapshot = createMetadataSnapshot(1);
        testEquals(emptySnapshot, equalEmptySnapshot, differentSnapshot);
        testHashCode(emptySnapshot, equalEmptySnapshot);
        testSerialization(emptySnapshot, equalEmptySnapshot, differentSnapshot);
    }

    private static FrontendShardDataTreeSnapshotMetadata createEmptyMetadataSnapshot() {
        return new FrontendShardDataTreeSnapshotMetadata(Collections.<FrontendClientMetadata> emptyList());
    }

    private static FrontendShardDataTreeSnapshotMetadata createMetadataSnapshot(final int size) {
        final List<FrontendClientMetadata> clients = new ArrayList<>();
        for (long i = 0; i < size; i++) {
            clients.add(createFrontedClientMetadata(i));
        }
        return new FrontendShardDataTreeSnapshotMetadata(clients);
    }

    private static FrontendClientMetadata createFrontedClientMetadata(final long i) {
        final String index = String.valueOf(i);
        final String indexName = "test_" + index;
        final FrontendIdentifier frontendIdentifier = FrontendIdentifier.create(MemberName.forName(indexName),
                FrontendType.forName(index));
        final ClientIdentifier clientIdentifier = ClientIdentifier.create(frontendIdentifier, i);

        final RangeSet<UnsignedLong> purgedHistories = TreeRangeSet.create();
        final Collection<FrontendHistoryMetadata> currentHistories = Collections.emptyList();
        return new FrontendClientMetadata(clientIdentifier, purgedHistories, currentHistories);
    }

    private static final <T> void testEquals(final T object, final T equalObject, final T differentObject) {
        assertTrue(object.equals(object));
        assertTrue(object.equals(equalObject));
        assertFalse(object.equals(null));
        assertFalse(object.equals("dummy"));
        assertFalse(object.equals(differentObject));
    }


    private static final <T> void testHashCode(final T object, final T equalObject) {
        assertEquals(object.hashCode(), equalObject.hashCode());
    }

    @SuppressWarnings("unchecked")
    private static <T> T copy(final T o) throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(o);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            return (T) ois.readObject();
        }
    }

    private static final <T> void testSerialization(final T object, final T equalObject, final T differentObject) throws Exception {
        assertTrue(object.equals(copy(object)));
        assertTrue(object.equals(copy(equalObject)));
        assertFalse(differentObject.equals(copy(object)));
    }

}
