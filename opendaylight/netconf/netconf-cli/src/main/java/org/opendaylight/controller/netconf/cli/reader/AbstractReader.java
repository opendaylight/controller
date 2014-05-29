package org.opendaylight.controller.netconf.cli.reader;

import java.io.IOException;
import java.util.List;

import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public abstract class AbstractReader<T extends DataSchemaNode> implements Reader<T> {

    protected ConsoleIO console;

    public AbstractReader(ConsoleIO console) {
        this.console = console;
    }

    @Override
    public List<Node<?>> read(T schemaNode) throws ReadingException {
        ConsoleContext ctx = getContext(schemaNode);
        console.enterContext(ctx);
        try {
            return readInner(schemaNode);
        } catch (IOException e) {
            throw new ReadingException("Unable to read data from input for " + schemaNode.getQName(), e);
        } finally {
            console.leaveContext();
        }
    }

    protected abstract List<Node<?>> readInner(T schemaNode) throws IOException, ReadingException;

    protected abstract ConsoleContext getContext(T schemaNode);

}
