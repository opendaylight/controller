package org.opendaylight.controller.cluster.access.commands;

import com.google.common.base.Optional;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Externalizable proxy for use with {@link ReadTransactionSuccess}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class ReadTransactionSuccessV1 extends AbstractTransactionSuccessProxy<ReadTransactionSuccess> {
    private static final long serialVersionUID = 1L;
    private Optional<NormalizedNode<?, ?>> data;

    public ReadTransactionSuccessV1() {
        // For Externalizable
    }

    ReadTransactionSuccessV1(final ReadTransactionSuccess request) {
        super(request);
        this.data = request.getData();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(data);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        data = (Optional<NormalizedNode<?, ?>>) in.readObject();
    }

    @Override
    protected ReadTransactionSuccess createSuccess(final TransactionIdentifier target, final long sequence) {
        return new ReadTransactionSuccess(target, sequence, data);
    }
}
