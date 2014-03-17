package org.opendaylight.controller.config.yang.doc.generator.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang2sources.spi.CodeGenerator;

public class ConfigYangDocGenerator implements CodeGenerator {

    @Override
    public Collection<File> generateSources(SchemaContext context, File outputBaseDir, Set<Module> currentModules)
            throws IOException {
        return new YangDocGenerator(outputBaseDir).generateDoc(currentModules);
    }

    @Override
    public void setAdditionalConfig(Map<String, String> arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLog(Log arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setMavenProject(MavenProject arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setResourceBaseDir(File arg0) {
        // TODO Auto-generated method stub

    }

}
