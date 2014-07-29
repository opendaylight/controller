package org.opendaylight.controller.cluster.datastore.utils;

import org.opendaylight.controller.cluster.datastore.node.utils.NodeIdentifierFactory;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: syedbahm
 */
public class InstanceIdentifierUtils {
  public static String getParentPath(String currentElementPath) {
    String parentPath = "";

    if (currentElementPath != null) {
      // FIXME: Use a guava splitter for performance
      String[] parentPaths = currentElementPath.split("/");
      if (parentPaths.length > 2) {
        for (int i = 0; i < parentPaths.length - 1; i++) {
          if (parentPaths[i].length() > 0) {
            parentPath += "/" + parentPaths[i];
          }
        }
      }
    }
    return parentPath;
  }

  public static YangInstanceIdentifier from(String path) {
    // FIXME: Use a guava splitter for performance
    String[] ids = path.split("/");

    List<YangInstanceIdentifier.PathArgument> pathArguments = new ArrayList<>();
    for (String nodeId : ids) {
      if (!"".equals(nodeId)) {
        pathArguments.add(NodeIdentifierFactory.getArgument(nodeId));
      }
    }
    final YangInstanceIdentifier instanceIdentifier =
        YangInstanceIdentifier.create(pathArguments);
    return instanceIdentifier;
  }
}
