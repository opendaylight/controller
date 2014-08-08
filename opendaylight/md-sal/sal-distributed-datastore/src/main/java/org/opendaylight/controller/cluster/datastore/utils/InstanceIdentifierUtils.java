package org.opendaylight.controller.cluster.datastore.utils;

import org.opendaylight.controller.cluster.datastore.node.utils.NodeIdentifierFactory;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: syedbahm
 */
public class InstanceIdentifierUtils {

    protected static final Logger logger = LoggerFactory
        .getLogger(InstanceIdentifierUtils.class);

    public static String getParentPath(String currentElementPath) {

        StringBuilder parentPath = new StringBuilder();

        if (currentElementPath != null) {
            String[] parentPaths = currentElementPath.split("/");
            if (parentPaths.length > 2) {
                for (int i = 0; i < parentPaths.length - 1; i++) {
                    if (parentPaths[i].length() > 0) {
                        parentPath.append( "/");
                        parentPath.append( parentPaths[i]);
                    }
                }
            }
        }
        return parentPath.toString();
    }

    @Deprecated
    public static YangInstanceIdentifier from(String path) {
        String[] ids = path.split("/");

        List<YangInstanceIdentifier.PathArgument> pathArguments =
            new ArrayList<>();
        for (String nodeId : ids) {
            if (!"".equals(nodeId)) {
                pathArguments
                    .add(NodeIdentifierFactory.getArgument(nodeId));
            }
        }
        final YangInstanceIdentifier instanceIdentifier =
            YangInstanceIdentifier.create(pathArguments);
        return instanceIdentifier;
    }

    /**
     * @deprecated Use {@link org.opendaylight.controller.cluster.datastore.util.InstanceIdentifierUtils} instead
     * @param path
     * @return
     */
    @Deprecated
    public static NormalizedNodeMessages.InstanceIdentifier toSerializable(YangInstanceIdentifier path){
        return org.opendaylight.controller.cluster.datastore.util.InstanceIdentifierUtils.toSerializable(path);
    }

    /**
     * @deprecated Use {@link org.opendaylight.controller.cluster.datastore.util.InstanceIdentifierUtils} instead
     * @param path
     * @return
     */
    @Deprecated
    public static YangInstanceIdentifier fromSerializable(NormalizedNodeMessages.InstanceIdentifier path){
        return org.opendaylight.controller.cluster.datastore.util.InstanceIdentifierUtils.fromSerializable(path);
    }
}
