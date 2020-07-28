/*
 * Copyright (c) 2020 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import akka.actor.ActorRef;
import akka.persistence.PersistentRepr;
import java.util.Arrays;

/**
 * This class is a custom version of akka's {@link PersistentRepr} which doesn't hold the payload of the whole entry but
 * rather a fragment of it. When working with the original PersistentRepr the payload can be easily
 * deserialized/serialized without any issue.
 * This is not the case for fragmented payload. After this fragment is created, no more serialization is needed nor
 * desired. Therefore the fragmented payload is kept specifically as byte[] and not Object.
 */
final class FragmentedPersistentRepr {

    private byte[] payloadFragment;
    private long sequenceNr;
    private String persistenceId;
    private String manifest;
    private boolean deleted;
    private ActorRef sender;
    private String writerUuid;

    FragmentedPersistentRepr(final byte[] payloadFragment, final long sequenceNr, final String persistenceId,
        final String manifest, final boolean deleted, final ActorRef sender, final String writerUuid) {

        this.payloadFragment = payloadFragment;
        this.sequenceNr = sequenceNr;
        this.persistenceId = persistenceId;
        this.manifest = manifest;
        this.deleted = deleted;
        this.sender = sender;
        this.writerUuid = writerUuid;
    }

    static FragmentedPersistentRepr apply(final byte[] payloadFragment, final long sequenceNr,
        final String persistenceId, final String manifest, final boolean deleted, final ActorRef sender,
        final String writerUuid) {
        return new FragmentedPersistentRepr(payloadFragment, sequenceNr, persistenceId, manifest, deleted, sender,
            writerUuid);
    }

    static FragmentedPersistentRepr applyFromRepr(final byte[] payloadFragment, final PersistentRepr repr) {
        return new FragmentedPersistentRepr(payloadFragment, repr.sequenceNr(), repr.persistenceId(), repr.manifest(),
            repr.deleted(), repr.sender(), repr.writerUuid());
    }

    PersistentRepr toPersistentRepr() {
        return PersistentRepr.apply(payloadFragment(), this.sequenceNr, this.persistenceId, this.manifest,
            this.deleted, this.sender, this.writerUuid);
    }

    byte[] payloadFragment() {
        return Arrays.copyOf(this.payloadFragment, this.payloadFragment.length);
    }

    String manifest() {
        return this.manifest;
    }

    String persistenceId() {
        return this.persistenceId;
    }

    long sequenceNr() {
        return this.sequenceNr;
    }

    boolean deleted() {
        return this.deleted;
    }

    ActorRef sender() {
        return this.sender;
    }

    String writerUuid() {
        return this.writerUuid;
    }

    FragmentedPersistentRepr withPayloadFragment(final byte[] newPayloadFragment) {
        this.payloadFragment = newPayloadFragment;
        return this;
    }

    FragmentedPersistentRepr withManifest(final String newManifest) {
        this.manifest = newManifest;
        return this;
    }

    FragmentedPersistentRepr update(final long newSequenceNr, final String newPersistenceId, final boolean newDeleted,
        final ActorRef newSender, final String newWriterUuid) {
        this.sequenceNr = newSequenceNr;
        this.persistenceId = newPersistenceId;
        this.deleted = newDeleted;
        this.sender = newSender;
        this.writerUuid = newWriterUuid;
        return this;
    }
}
