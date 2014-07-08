package org.opendaylight.controller.cluster.datastore.utils;

import org.opendaylight.controller.cluster.datastore.node.utils.NodeIdentifierFactory;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: syedbahm
 */
public class InstanceIdentifierUtils {
  public static String getParentPath(String currentElementPath) {
    String parentPath = "";

    if (currentElementPath != null) {
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

  public static InstanceIdentifier from(String path) {
    String[] ids = path.split("/");

    List<InstanceIdentifier.PathArgument> pathArguments = new ArrayList<>();
    for (String nodeId : ids) {
      if (!"".equals(nodeId)) {
        pathArguments.add(NodeIdentifierFactory.getArgument(nodeId));
      }
    }
    final InstanceIdentifier instanceIdentifier =
        new InstanceIdentifier(pathArguments);
    return instanceIdentifier;
  }
}
