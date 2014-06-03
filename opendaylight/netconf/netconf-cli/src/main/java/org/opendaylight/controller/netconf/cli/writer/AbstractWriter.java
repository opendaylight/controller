package org.opendaylight.controller.netconf.cli.writer;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.INDENT;

import java.io.IOException;
import java.util.List;

import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public abstract class AbstractWriter<T extends DataSchemaNode> implements Writer<T> {

    protected static final String OUTPUT_OPEN_NODE = " {";
    protected static final String OUTPUT_CLOSE_NODE = "}";

    protected String indent;
    protected ConsoleIO console;

    public AbstractWriter(final ConsoleIO console, final String indent) {
        this.indent = indent;
        this.console = console;
    }

    protected String indent() {
        return INDENT;
    }

    @Override
    public void write(final T dataSchemaNode, final List<Node<?>> dataNodes) throws WriteException {
        try {
            writeInner(dataSchemaNode, dataNodes);
        } catch (final IOException e) {
            throw new WriteException("Unable to write data to output for " + dataSchemaNode.getQName(), e);
        }
    }

    protected abstract void writeInner(final T dataSchemaNode, final List<Node<?>> dataNodes) throws IOException,
            WriteException;
}
