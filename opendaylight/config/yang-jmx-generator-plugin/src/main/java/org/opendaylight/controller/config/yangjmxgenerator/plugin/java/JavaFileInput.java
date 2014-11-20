package org.opendaylight.controller.config.yangjmxgenerator.plugin.java;

import com.google.common.base.Optional;
import java.util.List;

public interface JavaFileInput {

    FullyQualifiedName getFQN();

    Optional<String> getCopyright();

    Optional<String> getHeader();

    TypeName getType();

    Optional<String> getClassJavaDoc();

    List<String> getClassAnnotations();

    List<FullyQualifiedName> getExtends();

    List<FullyQualifiedName> getImplements();

    List<String> getBodyElements();

}
