package org.opendaylight.controller.netconf.cli.io;

import static org.opendaylight.controller.netconf.cli.io.IOUtil.SKIP;

import java.util.ArrayList;
import java.util.Collection;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.yangtools.yang.model.util.EnumerationType;

public class BaseDataHolderConsoleContext extends BaseConsoleContext<DataSchemaNode> {

    public BaseDataHolderConsoleContext(final DataSchemaNode dataSchemaNode) {
        super(dataSchemaNode);
    }

    @Override
    public Completer getCompleter() {
        TypeDefinition<?> dataType = null;
        if (dataSchemaNode instanceof LeafSchemaNode) {
            dataType = ((LeafSchemaNode) dataSchemaNode).getType();
        } else if (dataSchemaNode instanceof LeafListSchemaNode) {
            dataType = ((LeafListSchemaNode) dataSchemaNode).getType();
        }

        if (dataType instanceof EnumerationType) {
            final Collection<String> completerNames = new ArrayList<>();
            for (final EnumPair enumPair : ((EnumerationType) dataType).getValues()) {
                completerNames.add(enumPair.getName());
            }
            completerNames.add(SKIP);

            return new StringsCompleter(completerNames);
        }

        return super.getCompleter();
    }

}
