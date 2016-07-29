package org.opendaylight.controller.cluster.datastore.utils;

import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ClusterUtils {


    public static ShardIdentifier getShardIdentifier(final MemberName memberName, final DOMDataTreeIdentifier prefix) {
        return ShardIdentifier.create(getCleanShardName(prefix.getRootIdentifier()), memberName, prefix.getDatastoreType().name());
    }

    public static String getCleanShardName(final YangInstanceIdentifier path) {
        final StringBuilder builder = new StringBuilder();
        // TODO need a better mapping that includes namespace, but well need to cleanup the string beforehand
        path.getPathArguments().forEach(p -> {
            builder.append(p.getNodeType().getLocalName());
            builder.append("!");
        });
        return builder.toString();
    }

    private static void replaceAll(final StringBuilder builder, final String from, final String to) {
        int index = builder.indexOf(from);
        while (index != -1) {
            builder.replace(index, index + from.length(), to);
            index += to.length(); // Move to the end of the replacement
            index = builder.indexOf(from, index);
        }
    }
}
