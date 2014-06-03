package org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.serializer;

public class OutFormatter {

    public static final String INDENT = "  ";
    int indentLevel = -1;
    private String currentIndent = "";

    public String indent() {
        return currentIndent;
    }

    private void prepareIndent() {
        final StringBuilder output = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            output.append(INDENT);
        }
        currentIndent = output.toString();
    }

    public void increaseIndent() {
        indentLevel++;
        prepareIndent();
    }

    public void decreaseIndent() {
        indentLevel--;
        prepareIndent();
    }

}