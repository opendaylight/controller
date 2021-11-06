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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.primitives.UnsignedLong;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

public class FrontendShardDataTreeSnapshotMetadataTest {

    @Test
    public void testCreateMetadataSnapshotNullInput() {
        assertThrows(NullPointerException.class, () -> new FrontendShardDataTreeSnapshotMetadata(null));
    }

    @Test
    public void testCreateMetadataSnapshotEmptyInput() throws Exception {
        final FrontendShardDataTreeSnapshotMetadata emptyOrigSnapshot = createEmptyMetadataSnapshot();
        final FrontendShardDataTreeSnapshotMetadata emptyCopySnapshot = copy(emptyOrigSnapshot, 127);
        testMetadataSnapshotEqual(emptyOrigSnapshot, emptyCopySnapshot);
    }

    @Test
    public void testSerializeMetadataSnapshotWithOneClient() throws Exception {
        final FrontendShardDataTreeSnapshotMetadata origSnapshot = createMetadataSnapshot(1);
        final FrontendShardDataTreeSnapshotMetadata copySnapshot = copy(origSnapshot, 162);
        testMetadataSnapshotEqual(origSnapshot, copySnapshot);
    }

    @Test
    public void testSerializeMetadataSnapshotWithMoreClients() throws Exception {
        final FrontendShardDataTreeSnapshotMetadata origSnapshot = createMetadataSnapshot(5);
        final FrontendShardDataTreeSnapshotMetadata copySnapshot = copy(origSnapshot, 314);
        testMetadataSnapshotEqual(origSnapshot, copySnapshot);
    }

    private static void testMetadataSnapshotEqual(final FrontendShardDataTreeSnapshotMetadata origSnapshot,
            final FrontendShardDataTreeSnapshotMetadata copySnapshot) {

        final List<FrontendClientMetadata> origClientList = origSnapshot.getClients();
        final List<FrontendClientMetadata> copyClientList = copySnapshot.getClients();

        assertEquals(origClientList.size(), copyClientList.size());

        final Map<ClientIdentifier, FrontendClientMetadata> origIdent = new HashMap<>();
        final Map<ClientIdentifier, FrontendClientMetadata> copyIdent = new HashMap<>();
        origClientList.forEach(client -> origIdent.put(client.getIdentifier(), client));
        origClientList.forEach(client -> copyIdent.put(client.getIdentifier(), client));

        assertTrue(origIdent.keySet().containsAll(copyIdent.keySet()));
        assertTrue(copyIdent.keySet().containsAll(origIdent.keySet()));

        origIdent.values().forEach(client -> {
            final FrontendClientMetadata copyClient = copyIdent.get(client.getIdentifier());
            testObject(client.getIdentifier(), copyClient.getIdentifier());
            assertEquals(client.getPurgedHistories(), copyClient.getPurgedHistories());
            assertEquals(client.getCurrentHistories(), copyClient.getCurrentHistories());
        });
    }

    private static FrontendShardDataTreeSnapshotMetadata createEmptyMetadataSnapshot() {
        return new FrontendShardDataTreeSnapshotMetadata(List.of());
    }

    private static FrontendShardDataTreeSnapshotMetadata createMetadataSnapshot(final int size) {
        final List<FrontendClientMetadata> clients = new ArrayList<>(size);
        for (long i = 0; i < size; i++) {
            clients.add(createFrontedClientMetadata(i));
        }
        return new FrontendShardDataTreeSnapshotMetadata(clients);
    }

    private static FrontendClientMetadata createFrontedClientMetadata(final long num) {
        final String index = String.valueOf(num);
        final String indexName = "test_" + index;
        final FrontendIdentifier frontendIdentifier = FrontendIdentifier.create(MemberName.forName(indexName),
                FrontendType.forName(index));
        final ClientIdentifier clientIdentifier = ClientIdentifier.create(frontendIdentifier, num);

        final RangeSet<UnsignedLong> purgedHistories = TreeRangeSet.create();
        purgedHistories.add(Range.closedOpen(UnsignedLong.ZERO, UnsignedLong.ONE));

        final Set<FrontendHistoryMetadata> currentHistories = Set.of(
            new FrontendHistoryMetadata(num, num, true, ImmutableMap.of(UnsignedLong.ZERO, Boolean.TRUE),
                purgedHistories));

        return new FrontendClientMetadata(clientIdentifier, purgedHistories, currentHistories);
    }

    private static <T> void testObject(final T object, final T equalObject) {
        assertEquals(object.hashCode(), equalObject.hashCode());
        assertTrue(object.equals(object));
        assertTrue(object.equals(equalObject));
        assertFalse(object.equals(null));
        assertFalse(object.equals("dummy"));
    }

    @SuppressWarnings("unchecked")
    private static <T> T copy(final T obj, final int expectedSize) throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
        }

        final byte[] bytes = bos.toByteArray();
        assertEquals(expectedSize, bytes.length);

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) ois.readObject();
        }
    }
}
