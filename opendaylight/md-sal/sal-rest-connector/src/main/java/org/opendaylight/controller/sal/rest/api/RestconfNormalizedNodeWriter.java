package org.opendaylight.controller.sal.rest.api;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public interface RestconfNormalizedNodeWriter extends Flushable, Closeable {

    RestconfNormalizedNodeWriter write(final NormalizedNode<?, ?> node) throws IOException;
}
