/*
 * Copyright 2015-2022 Open Networking Foundation and others.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Segment file utility.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
final class JournalSegmentFile {
    private static final char PART_SEPARATOR = '-';
    private static final char EXTENSION_SEPARATOR = '.';
    private static final String EXTENSION = "log";

    private final @NonNull JournalSegmentDescriptor descriptor;
    private final @NonNull Path path;

    private final RandomAccessFile file;

    private JournalSegmentFile(final Path path, final JournalSegmentDescriptor descriptor,
            final RandomAccessFile file) {
        this.path = requireNonNull(path);
        this.descriptor = requireNonNull(descriptor);
        this.file = requireNonNull(file);
    }

    static @NonNull JournalSegmentFile createNew(final String name, final File directory,
            final JournalSegmentDescriptor descriptor) throws IOException {
        final var file = createSegmentFile(name, directory, descriptor.id());
        final var raf = new RandomAccessFile(file, "rw");
        try {
            raf.setLength(descriptor.maxSegmentSize());
            raf.write(descriptor.toArray());
        } catch (IOException e) {
            raf.close();
            throw e;
        }
        return new JournalSegmentFile(file.toPath(), descriptor, raf);
    }

    static @NonNull JournalSegmentFile openExisting(final Path path) throws IOException {
        final var raf = new RandomAccessFile(path.toFile(), "rw");
        final JournalSegmentDescriptor descriptor;
        try {
            // read the descriptor
            descriptor = JournalSegmentDescriptor.readFrom(raf.getChannel());
        } catch (IOException e) {
            raf.close();
            throw e;
        }
        return new JournalSegmentFile(path, descriptor, raf);
    }

    /**
     * Returns the segment file.
     *
     * @return The segment file.
     */
    @NonNull Path path() {
        return path;
    }

    /**
     * Returns the segment descriptor.
     *
     * @return The segment descriptor.
     */
    @NonNull JournalSegmentDescriptor descriptor() {
        return descriptor;
    }

    int maxSize() {
        return descriptor.maxSegmentSize();
    }

    int size() throws IOException {
        return (int) file.length();
    }

    FileChannel channel() {
        return file.getChannel();
    }

    void close() throws IOException {
        file.close();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("path", path).add("descriptor", descriptor).toString();
    }

    /**
     * Returns a boolean value indicating whether the given file appears to be a parsable segment file.
     *
     * @throws NullPointerException if {@code file} is null
     */
    public static boolean isSegmentFile(final String name, final File file) {
        return isSegmentFile(name, file.getName());
    }

    /**
     * Returns a boolean value indicating whether the given file appears to be a parsable segment file.
     *
     * @param journalName the name of the journal
     * @param fileName the name of the file to check
     * @throws NullPointerException if {@code file} is null
     */
    public static boolean isSegmentFile(final String journalName, final String fileName) {
        requireNonNull(journalName, "journalName cannot be null");
        requireNonNull(fileName, "fileName cannot be null");

        int partSeparator = fileName.lastIndexOf(PART_SEPARATOR);
        int extensionSeparator = fileName.lastIndexOf(EXTENSION_SEPARATOR);

        if (extensionSeparator == -1 || partSeparator == -1 || extensionSeparator < partSeparator
            || !fileName.endsWith(EXTENSION)) {
            return false;
        }

        for (int i = partSeparator + 1; i < extensionSeparator; i++) {
            if (!Character.isDigit(fileName.charAt(i))) {
                return false;
            }
        }

        return fileName.startsWith(journalName);
    }

    /**
     * Creates a segment file for the given directory, log name, segment ID, and segment version.
     */
    static File createSegmentFile(final String name, final File directory, final long id) {
        return new File(directory, String.format("%s-%d.log", requireNonNull(name, "name cannot be null"), id));
    }
}
