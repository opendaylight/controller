package org.opendaylight.controller.netconf.cli.reader;

import java.io.IOException;
import java.util.List;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public abstract class AbstractReader<T extends DataSchemaNode> implements Reader<T> {

    protected ConsoleIO console;

    public AbstractReader(final ConsoleIO console) {
        this.console = console;
    }

    @Override
    public List<Node<?>> read(final T schemaNode) throws ReadingException {
        final ConsoleContext ctx = getContext(schemaNode);
        console.enterContext(ctx);
        try {
            return readInner(schemaNode);
        } catch (final IOException e) {
            throw new ReadingException("Unable to read data from input for " + schemaNode.getQName(), e);
        } finally {
            console.leaveContext();
        }
    }

    protected abstract List<Node<?>> readInner(T schemaNode) throws IOException, ReadingException;

    protected abstract ConsoleContext getContext(T schemaNode);

    protected boolean isRawValueAmongProposedValues(final String rawValue, final Completer completer) {
        if (completer instanceof StringsCompleter) {
            for (final String proposedValue : ((StringsCompleter) completer).getStrings()) {
                if (rawValue.equals(proposedValue)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }
}
