package org.opendaylight.controller.netconf.cli.writer;

public class OutFormatter {

    public static final String INDENT_STEP = "  ";
    public static final String COMPOSITE_OPEN_NODE = " {";
    public static final String COMPOSITE_CLOSE_NODE = "}";
    public static final String NEW_LINE = "\n";

    int indentLevel = -1;
    private String currentIndent = "";

    public OutFormatter indent(final StringBuilder buffer) {
        buffer.append(currentIndent);
        return this;
    }

//    public OutFormatter increaseAndIndent(final StringBuilder buffer) {
//        increaseIndent();
//        buffer.append(currentIndent);
//        return this;
//    }
//
//    public OutFormatter decreasendIndent(final StringBuilder buffer) {
//        decreaseIndent();
//        buffer.append(currentIndent);
//        return this;
//    }

    public OutFormatter openComposite(final StringBuilder buffer) {
        buffer.append(COMPOSITE_OPEN_NODE);
        return this;
    }

    public OutFormatter closeCompositeWithIndent(final StringBuilder buffer) {
        buffer.append(currentIndent);
        buffer.append(COMPOSITE_CLOSE_NODE);
        return this;
    }

    public OutFormatter newLine(final StringBuilder buffer) {
        buffer.append(NEW_LINE);
        return this;
    }

    private void prepareIndent() {
        final StringBuilder output = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            output.append(INDENT_STEP);
        }
        currentIndent = output.toString();
    }

    public OutFormatter increaseIndent() {
        indentLevel++;
        prepareIndent();
        return this;
    }

    public OutFormatter decreaseIndent() {
        indentLevel--;
        prepareIndent();
        return this;
    }

    public OutFormatter addStringWithIndent(final StringBuilder buffer, final String value) {
        indent(buffer);
        buffer.append(value);
        return this;
    }
}