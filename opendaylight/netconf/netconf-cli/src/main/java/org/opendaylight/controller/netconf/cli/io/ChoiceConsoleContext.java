package org.opendaylight.controller.netconf.cli.io;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public class ChoiceConsoleContext extends BaseConsoleContext {

    protected Map<String, ChoiceCaseNode> availableCases = new HashMap<String, ChoiceCaseNode>();

    public ChoiceConsoleContext(DataSchemaNode schemaNode) {
        super(schemaNode);
    }

    public ChoiceCaseNode getCaseSchemaNode(String key) {
        return availableCases.get(key);
    }
}