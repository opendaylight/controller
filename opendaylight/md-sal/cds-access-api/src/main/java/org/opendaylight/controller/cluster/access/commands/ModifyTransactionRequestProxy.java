package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Externalizable proxy for use with {@link ExistsTransactionRequest}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class ModifyTransactionRequestProxy extends AbstractTransactionRequestProxy<ModifyTransactionRequest> {
    private static final long serialVersionUID = 1L;
    private List<TransactionModification> modifications;
    private Optional<PersistenceProtocol> protocol;

    public ModifyTransactionRequestProxy() {
        // For Externalizable
    }

    ModifyTransactionRequestProxy(final ModifyTransactionRequest request) {
        super(request);
        this.modifications = Preconditions.checkNotNull(request.getModifications());
        this.protocol = request.getPersistenceProtocol();
    }


    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        final int size = in.readInt();
        if (size != 0) {
            modifications = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                modifications.add((TransactionModification) in.readObject());
            }
        }
        protocol = Optional.ofNullable(PersistenceProtocol.readFrom(in));
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);

        out.writeInt(modifications.size());
        for (TransactionModification op : modifications) {
            out.writeObject(op);
        }

        out.writeByte(PersistenceProtocol.byteValue(protocol.orElse(null)));
    }

    @Override
    protected ModifyTransactionRequest createRequest(final TransactionIdentifier target, final long sequence,
            final ActorRef replyTo) {
        return new ModifyTransactionRequest(target, sequence, replyTo, modifications, protocol.orElse(null));
    }
}
