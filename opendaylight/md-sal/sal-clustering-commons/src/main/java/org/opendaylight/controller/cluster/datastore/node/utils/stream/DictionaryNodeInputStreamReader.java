package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.base.Preconditions;
import java.io.DataInput;
import java.io.IOException;
import org.opendaylight.yangtools.yang.common.QName;

final class DictionaryNodeInputStreamReader extends AbstractNormalizedNodeDataInput {
    DictionaryNodeInputStreamReader(final DataInput input, final NormalizedNodeInputDictionary dictionary) throws IOException {
        super(input, refreshDictionary(input, dictionary));
    }

    private static NormalizedNodeInputDictionary refreshDictionary(final DataInput input,
            final NormalizedNodeInputDictionary dictionary) throws IOException {
        final byte action = input.readByte();
        switch (action) {
            case TokenTypes.KEEP_DICTIONARY:
                Preconditions.checkArgument(dictionary != null, "Dictionary reuse is requested, but no dictionary present");
                return dictionary;
            case TokenTypes.RESET_DICTIONARY:
                return new NormalizedNodeInputDictionary();
            default:
                throw new InvalidNormalizedNodeStreamException("Unknown dictionary action " + action);
        }
    }

    @Override
    protected QName readQName() throws IOException {
        final byte token = readByte();
        switch (token) {
            case TokenTypes.IS_NULL_VALUE:
                return null;
            case TokenTypes.QNAME_DEFINITION:
                final QName qname = super.readQName();
                dictionary().storeQName(qname);
                return qname;
            case TokenTypes.QNAME_REFERENCE:
                return dictionary().lookupQName(readInt());
            default:
                throw new InvalidNormalizedNodeStreamException("Invalid QName token " + token);
        }
    }
}
