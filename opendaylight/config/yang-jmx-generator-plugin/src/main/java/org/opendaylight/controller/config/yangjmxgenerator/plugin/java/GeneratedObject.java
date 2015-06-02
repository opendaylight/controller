package org.opendaylight.controller.config.yangjmxgenerator.plugin.java;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.StringUtil;

public class GeneratedObject {

    private final FullyQualifiedName fqn;
    private final String content;

    public GeneratedObject(FullyQualifiedName fqn, String content) {
        this.fqn = checkNotNull(fqn);
        this.content = StringUtil.formatJavaSource(checkNotNull(content));
    }

    public FullyQualifiedName getFQN(){
        return fqn;
    }

    public String getContent() {
        return content;
    }

    public Optional<Entry<FullyQualifiedName,File>> persist(File srcDirectory, boolean overwrite) throws IOException {
        File dstFile = fqn.toFile(srcDirectory);
        if (overwrite || !dstFile.exists()) {
            Files.createParentDirs(dstFile);
            Files.touch(dstFile);
            Files.write(content, dstFile, Charset.defaultCharset());
            return Optional.of(Maps.immutableEntry(fqn, dstFile));
        } else {
            return Optional.absent();
        }
    }

    public Optional<Entry<FullyQualifiedName,File>> persist(File srcDirectory) throws IOException {
        return persist(srcDirectory, true);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "fqn=" + fqn +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GeneratedObject that = (GeneratedObject) o;

        return fqn.equals(that.fqn);

    }

    @Override
    public int hashCode() {
        return fqn.hashCode();
    }
}
