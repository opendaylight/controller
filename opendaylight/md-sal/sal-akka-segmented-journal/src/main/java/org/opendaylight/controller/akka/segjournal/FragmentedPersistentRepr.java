package org.opendaylight.controller.akka.segjournal;

import akka.actor.ActorRef;
import akka.persistence.PersistentRepr;
import java.util.Arrays;

final class FragmentedPersistentRepr {

    private byte[] payloadFragment;
    private long sequenceNr;
    private String persistenceId;
    private String manifest;
    private boolean deleted;
    private ActorRef sender;
    private String writerUuid;
    private long timestamp;

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

    static FragmentedPersistentRepr apply(final byte[] payloadFragment, final long sequenceNr, final String persistenceId,
        final String manifest, final boolean deleted, final ActorRef sender, final String writerUuid) {
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

    long timestamp() {
        return this.timestamp;
    }

    boolean deleted() {
        return this.deleted;
    }

    ActorRef sender() {
        return this.sender;
    }

    FragmentedPersistentRepr withTimestamp(long newTimestamp) {
        this.timestamp = newTimestamp;
        return this;
    }

    String writerUuid() {
        return this.writerUuid;
    }

    FragmentedPersistentRepr withPayloadFragment(final byte[] payloadFragment) {
        this.payloadFragment = payloadFragment;
        return this;
    }

    FragmentedPersistentRepr withManifest(final String manifest) {
        this.manifest = manifest;
        return this;
    }

    FragmentedPersistentRepr update(final long sequenceNr, final String persistenceId, final boolean deleted,
        final ActorRef sender, final String writerUuid) {
        this.sequenceNr = sequenceNr;
        this.persistenceId = persistenceId;
        this.deleted = deleted;
        this.sender = sender;
        this.writerUuid = writerUuid;
        return this;
    }
}
