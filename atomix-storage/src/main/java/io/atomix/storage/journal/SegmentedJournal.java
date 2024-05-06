/*
 * Copyright 2017-2022 Open Networking Foundation and others.  All rights reserved.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Segmented journal.
 */
public final class SegmentedJournal<E> implements Journal<E> {
  private static final Logger LOG = LoggerFactory.getLogger(SegmentedJournal.class);
  private static final int SEGMENT_BUFFER_FACTOR = 3;

  private final String name;
  private final StorageLevel storageLevel;
  private final File directory;
  private final JournalSerializer<E> serializer;
  private final int maxSegmentSize;
  private final int maxEntrySize;
  private final int maxEntriesPerSegment;
  private final double indexDensity;
  private final boolean flushOnCommit;
  private final SegmentedJournalWriter<E> writer;
  private volatile long commitIndex;

  private final ConcurrentNavigableMap<Long, JournalSegment> segments = new ConcurrentSkipListMap<>();
  private final Collection<SegmentedJournalReader<?>> readers = ConcurrentHashMap.newKeySet();

  // null when closed
  private JournalSegment currentSegment;

  public SegmentedJournal(
      String name,
      StorageLevel storageLevel,
      File directory,
      JournalSerdes namespace,
      int maxSegmentSize,
      int maxEntrySize,
      int maxEntriesPerSegment,
      double indexDensity,
      boolean flushOnCommit) {
    this.name = requireNonNull(name, "name cannot be null");
    this.storageLevel = requireNonNull(storageLevel, "storageLevel cannot be null");
    this.directory = requireNonNull(directory, "directory cannot be null");
    this.serializer = JournalSerializer.wrap(requireNonNull(namespace, "namespace cannot be null"));
    this.maxSegmentSize = maxSegmentSize;
    this.maxEntrySize = maxEntrySize;
    this.maxEntriesPerSegment = maxEntriesPerSegment;
    this.indexDensity = indexDensity;
    this.flushOnCommit = flushOnCommit;
    open();
    this.writer = new SegmentedJournalWriter<>(this);
  }

  /**
   * Returns the segment file name prefix.
   *
   * @return The segment file name prefix.
   */
  public String name() {
    return name;
  }

  /**
   * Returns the storage directory.
   * <p>
   * The storage directory is the directory to which all segments write files. Segment files for multiple logs may be
   * stored in the storage directory, and files for each log instance will be identified by the {@code prefix} provided
   * when the log is opened.
   *
   * @return The storage directory.
   */
  public File directory() {
    return directory;
  }

  /**
   * Returns the storage level.
   * <p>
   * The storage level dictates how entries within individual journal segments should be stored.
   *
   * @return The storage level.
   */
  public StorageLevel storageLevel() {
    return storageLevel;
  }

  /**
   * Returns the maximum journal segment size.
   * <p>
   * The maximum segment size dictates the maximum size any segment in a segment may consume in bytes.
   *
   * @return The maximum segment size in bytes.
   */
  public int maxSegmentSize() {
    return maxSegmentSize;
  }

  /**
   * Returns the maximum journal entry size.
   * <p>
   * The maximum entry size dictates the maximum size any entry in the segment may consume in bytes.
   *
   * @return the maximum entry size in bytes
   */
  public int maxEntrySize() {
    return maxEntrySize;
  }

  /**
   * Returns the maximum number of entries per segment.
   * <p>
   * The maximum entries per segment dictates the maximum number of entries that are allowed to be stored in any segment
   * in a journal.
   *
   * @return The maximum number of entries per segment.
   * @deprecated since 3.0.2
   */
  @Deprecated
  public int maxEntriesPerSegment() {
    return maxEntriesPerSegment;
  }

  /**
   * Returns serializer instance.
   *
   * @return serializer instance
   */
  JournalSerializer<E> serializer() {
    return serializer;
  }

  /**
   * Returns the total size of the journal.
   *
   * @return the total size of the journal
   */
  public long size() {
    return segments.values().stream()
        .mapToLong(segment -> {
          try {
            return segment.file().size();
          } catch (IOException e) {
            throw new StorageException(e);
          }
        })
        .sum();
  }

  @Override
  public long lastIndex() {
    final var segment = getLastSegment();
    return segment != null ? segment.lastIndex() : 0;
  }

  @Override
  public JournalWriter<E> writer() {
    return writer;
  }

  /**
   * Opens a new Raft log reader with the given reader mode.
   *
   * @param index The index from which to begin reading entries.
   * @param mode The mode in which to read entries.
   * @return The Raft log reader.
   */
  @Override
  public JournalReader<E> openReader(long index, JournalReader.Mode mode) {
    final var segment = getSegment(index);
    final var reader = switch (mode) {
      case ALL -> new SegmentedJournalReader<>(this, segment);
      case COMMITS -> new CommitsSegmentJournalReader<>(this, segment);
    };

    // Forward reader to specified index
    long next = reader.getNextIndex();
    while (index > next && reader.tryAdvance()) {
        next = reader.getNextIndex();
    }

    readers.add(reader);
    return reader;
  }

  /**
   * Opens the segments.
   */
  private synchronized void open() {
    // Load existing log segments from disk.
    for (var segment : loadSegments()) {
      segments.put(segment.firstIndex(), segment);
    }

    // If a segment doesn't already exist, create an initial segment starting at index 1.
    if (!segments.isEmpty()) {
      currentSegment = segments.lastEntry().getValue();
    } else {
      currentSegment = createSegment(1, 1);
      segments.put(1L, currentSegment);
    }
  }

  /**
   * Asserts that the manager is open.
   *
   * @throws IllegalStateException if the segment manager is not open
   */
  private void assertOpen() {
    checkState(currentSegment != null, "journal not open");
  }

  /**
   * Asserts that enough disk space is available to allocate a new segment.
   */
  private void assertDiskSpace() {
    if (directory().getUsableSpace() < maxSegmentSize() * SEGMENT_BUFFER_FACTOR) {
      throw new StorageException.OutOfDiskSpace("Not enough space to allocate a new journal segment");
    }
  }

  /**
   * Resets the current segment, creating a new segment if necessary.
   */
  private synchronized void resetCurrentSegment() {
    final var lastSegment = getLastSegment();
    if (lastSegment == null) {
      currentSegment = createSegment(1, 1);
      segments.put(1L, currentSegment);
    } else {
      currentSegment = lastSegment;
    }
  }

  /**
   * Resets and returns the first segment in the journal.
   *
   * @param index the starting index of the journal
   * @return the first segment
   */
  JournalSegment resetSegments(long index) {
    assertOpen();

    // If the index already equals the first segment index, skip the reset.
    final var firstSegment = getFirstSegment();
    if (index == firstSegment.firstIndex()) {
      return firstSegment;
    }

    segments.values().forEach(JournalSegment::delete);
    segments.clear();

    currentSegment = createSegment(1, index);
    segments.put(index, currentSegment);
    return currentSegment;
  }

  private static @Nullable JournalSegment segmentOf(@Nullable Entry<Long, JournalSegment> entry) {
      return entry == null ? null : entry.getValue();
  }

  /**
   * Returns the first segment in the log.
   *
   * @throws IllegalStateException if the segment manager is not open
   */
  JournalSegment getFirstSegment() {
    assertOpen();
    return segmentOf(segments.firstEntry());
  }

  /**
   * Returns the last segment in the log.
   *
   * @throws IllegalStateException if the segment manager is not open
   */
  JournalSegment getLastSegment() {
    assertOpen();
    return segmentOf(segments.lastEntry());
  }

  /**
   * Creates and returns the next segment.
   *
   * @return The next segment.
   * @throws IllegalStateException if the segment manager is not open
   */
  synchronized JournalSegment getNextSegment() {
    assertOpen();
    assertDiskSpace();

    final var index = currentSegment.lastIndex() + 1;
    final var lastSegment = getLastSegment();
    currentSegment = createSegment(lastSegment != null ? lastSegment.file().segmentId() + 1 : 1, index);
    segments.put(index, currentSegment);
    return currentSegment;
  }

  /**
   * Returns the segment following the segment with the given ID.
   *
   * @param index The segment index with which to look up the next segment.
   * @return The next segment for the given index.
   */
  JournalSegment getNextSegment(long index) {
    return segmentOf(segments.higherEntry(index));
  }

  /**
   * Returns the segment for the given index.
   *
   * @param index The index for which to return the segment.
   * @throws IllegalStateException if the segment manager is not open
   */
  synchronized JournalSegment getSegment(long index) {
    assertOpen();
    // Check if the current segment contains the given index first in order to prevent an unnecessary map lookup.
    if (currentSegment != null && index > currentSegment.firstIndex()) {
      return currentSegment;
    }

    // If the index is in another segment, get the entry with the next lowest first index.
    final var segment = segments.floorEntry(index);
    if (segment != null) {
      return segment.getValue();
    }
    return getFirstSegment();
  }

  /**
   * Removes a segment.
   *
   * @param segment The segment to remove.
   */
  synchronized void removeSegment(JournalSegment segment) {
    segments.remove(segment.firstIndex());
    segment.delete();
    resetCurrentSegment();
  }

  /**
   * Creates a new segment.
   */
  JournalSegment createSegment(long id, long index) {
    final JournalSegmentFile file;
    try {
      file = JournalSegmentFile.createNew(name, directory, JournalSegmentDescriptor.builder()
          .withId(id)
          .withIndex(index)
          .withMaxSegmentSize(maxSegmentSize)
          .withMaxEntries(maxEntriesPerSegment)
          .withUpdated(System.currentTimeMillis())
          .build());
    } catch (IOException e) {
      throw new StorageException(e);
    }

    final var segment = new JournalSegment(file, storageLevel, maxEntrySize, indexDensity);
    LOG.debug("Created segment: {}", segment);
    return segment;
  }

  /**
   * Loads all segments from disk.
   *
   * @return A collection of segments for the log.
   */
  protected Collection<JournalSegment> loadSegments() {
    // Ensure log directories are created.
    directory.mkdirs();

    final var segments = new TreeMap<Long, JournalSegment>();

    // Iterate through all files in the log directory.
    for (var file : directory.listFiles(File::isFile)) {

      // If the file looks like a segment file, attempt to load the segment.
      if (JournalSegmentFile.isSegmentFile(name, file)) {
        final JournalSegmentFile segmentFile;
        try {
          segmentFile = JournalSegmentFile.openExisting(file.toPath());
        } catch (IOException e) {
          throw new StorageException(e);
        }

        // Load the segment.
        LOG.debug("Loaded disk segment: {} ({})", segmentFile.segmentId(), segmentFile.path());

        // Add the segment to the segments list.
        final var segment = new JournalSegment(segmentFile, storageLevel, maxEntrySize, indexDensity);
        segments.put(segment.firstIndex(), segment);
      }
    }

    // Verify that all the segments in the log align with one another.
    JournalSegment previousSegment = null;
    boolean corrupted = false;
    final var iterator = segments.entrySet().iterator();
    while (iterator.hasNext()) {
      final var segment = iterator.next().getValue();
      if (previousSegment != null && previousSegment.lastIndex() != segment.firstIndex() - 1) {
        LOG.warn("Journal is inconsistent. {} is not aligned with prior segment {}", segment.file().path(),
            previousSegment.file().path());
        corrupted = true;
      }
      if (corrupted) {
        segment.delete();
        iterator.remove();
      }
      previousSegment = segment;
    }

    return segments.values();
  }

  /**
   * Resets journal readers to the given head.
   *
   * @param index The index at which to reset readers.
   */
  void resetHead(long index) {
    for (var reader : readers) {
      if (reader.getNextIndex() < index) {
        reader.reset(index);
      }
    }
  }

  /**
   * Resets journal readers to the given tail.
   *
   * @param index The index at which to reset readers.
   */
  void resetTail(long index) {
    for (var reader : readers) {
      if (reader.getNextIndex() >= index) {
        reader.reset(index);
      }
    }
  }

  void closeReader(SegmentedJournalReader<E> reader) {
    readers.remove(reader);
  }

  /**
   * Returns a boolean indicating whether a segment can be removed from the journal prior to the given index.
   *
   * @param index the index from which to remove segments
   * @return indicates whether a segment can be removed from the journal
   */
  public boolean isCompactable(long index) {
    final var compactableIndex = getCompactableIndex(index);
    return compactableIndex != 0 && !segments.headMap(compactableIndex).isEmpty();
  }

  /**
   * Returns the index of the last segment in the log.
   *
   * @param index the compaction index
   * @return the starting index of the last segment in the log
   */
  public long getCompactableIndex(long index) {
    final var segment = segments.floorEntry(index);
    return segment != null ? segment.getValue().firstIndex() : 0;
  }

  /**
   * Compacts the journal up to the given index.
   * <p>
   * The semantics of compaction are not specified by this interface.
   *
   * @param index The index up to which to compact the journal.
   */
  public void compact(long index) {
    final var firstIndex = getCompactableIndex(index);
    if (firstIndex != 0) {
      final var compactSegments = segments.headMap(firstIndex);
      if (!compactSegments.isEmpty()) {
        LOG.debug("{} - Compacting {} segment(s)", name, compactSegments.size());
        compactSegments.values().forEach(JournalSegment::delete);
        compactSegments.clear();
        resetHead(firstIndex);
      }
    }
  }

  @Override
  public void close() {
    if (currentSegment != null) {
      currentSegment = null;
      segments.values().forEach(JournalSegment::close);
      segments.clear();
    }
  }

  /**
   * Returns whether {@code flushOnCommit} is enabled for the log.
   *
   * @return Indicates whether {@code flushOnCommit} is enabled for the log.
   */
  boolean isFlushOnCommit() {
    return flushOnCommit;
  }

  /**
   * Commits entries up to the given index.
   *
   * @param index The index up to which to commit entries.
   */
  void setCommitIndex(long index) {
    this.commitIndex = index;
  }

  /**
   * Returns the Raft log commit index.
   *
   * @return The Raft log commit index.
   */
  long getCommitIndex() {
    return commitIndex;
  }

  /**
   * Returns a new Raft log builder.
   *
   * @return A new Raft log builder.
   */
  public static <E> Builder<E> builder() {
    return new Builder<>();
  }

  /**
   * Raft log builder.
   */
  public static final class Builder<E> {
    private static final boolean DEFAULT_FLUSH_ON_COMMIT = false;
    private static final String DEFAULT_NAME = "atomix";
    private static final String DEFAULT_DIRECTORY = System.getProperty("user.dir");
    private static final int DEFAULT_MAX_SEGMENT_SIZE = 1024 * 1024 * 32;
    private static final int DEFAULT_MAX_ENTRY_SIZE = 1024 * 1024;
    private static final int DEFAULT_MAX_ENTRIES_PER_SEGMENT = 1024 * 1024;
    private static final double DEFAULT_INDEX_DENSITY = .005;

    private String name = DEFAULT_NAME;
    private StorageLevel storageLevel = StorageLevel.DISK;
    private File directory = new File(DEFAULT_DIRECTORY);
    private JournalSerdes namespace;
    private int maxSegmentSize = DEFAULT_MAX_SEGMENT_SIZE;
    private int maxEntrySize = DEFAULT_MAX_ENTRY_SIZE;
    private int maxEntriesPerSegment = DEFAULT_MAX_ENTRIES_PER_SEGMENT;
    private double indexDensity = DEFAULT_INDEX_DENSITY;
    private boolean flushOnCommit = DEFAULT_FLUSH_ON_COMMIT;

    Builder() {
      // Hidden on purpose
    }

    /**
     * Sets the storage name.
     *
     * @param name The storage name.
     * @return The storage builder.
     */
    public Builder<E> withName(String name) {
      this.name = requireNonNull(name, "name cannot be null");
      return this;
    }

    /**
     * Sets the log storage level, returning the builder for method chaining.
     * <p>
     * The storage level indicates how individual entries should be persisted in the journal.
     *
     * @param storageLevel The log storage level.
     * @return The storage builder.
     */
    public Builder<E> withStorageLevel(StorageLevel storageLevel) {
      this.storageLevel = requireNonNull(storageLevel, "storageLevel cannot be null");
      return this;
    }

    /**
     * Sets the log directory, returning the builder for method chaining.
     * <p>
     * The log will write segment files into the provided directory.
     *
     * @param directory The log directory.
     * @return The storage builder.
     * @throws NullPointerException If the {@code directory} is {@code null}
     */
    public Builder<E> withDirectory(String directory) {
      return withDirectory(new File(requireNonNull(directory, "directory cannot be null")));
    }

    /**
     * Sets the log directory, returning the builder for method chaining.
     * <p>
     * The log will write segment files into the provided directory.
     *
     * @param directory The log directory.
     * @return The storage builder.
     * @throws NullPointerException If the {@code directory} is {@code null}
     */
    public Builder<E> withDirectory(File directory) {
      this.directory = requireNonNull(directory, "directory cannot be null");
      return this;
    }

    /**
     * Sets the journal namespace, returning the builder for method chaining.
     *
     * @param namespace The journal serializer.
     * @return The journal builder.
     */
    public Builder<E> withNamespace(JournalSerdes namespace) {
      this.namespace = requireNonNull(namespace, "namespace cannot be null");
      return this;
    }

    /**
     * Sets the maximum segment size in bytes, returning the builder for method chaining.
     * <p>
     * The maximum segment size dictates when logs should roll over to new segments. As entries are written to a segment
     * of the log, once the size of the segment surpasses the configured maximum segment size, the log will create a new
     * segment and append new entries to that segment.
     * <p>
     * By default, the maximum segment size is {@code 1024 * 1024 * 32}.
     *
     * @param maxSegmentSize The maximum segment size in bytes.
     * @return The storage builder.
     * @throws IllegalArgumentException If the {@code maxSegmentSize} is not positive
     */
    public Builder<E> withMaxSegmentSize(int maxSegmentSize) {
      checkArgument(maxSegmentSize > JournalSegmentDescriptor.BYTES,
          "maxSegmentSize must be greater than " + JournalSegmentDescriptor.BYTES);
      this.maxSegmentSize = maxSegmentSize;
      return this;
    }

    /**
     * Sets the maximum entry size in bytes, returning the builder for method chaining.
     *
     * @param maxEntrySize the maximum entry size in bytes
     * @return the storage builder
     * @throws IllegalArgumentException if the {@code maxEntrySize} is not positive
     */
    public Builder<E> withMaxEntrySize(int maxEntrySize) {
      checkArgument(maxEntrySize > 0, "maxEntrySize must be positive");
      this.maxEntrySize = maxEntrySize;
      return this;
    }

    /**
     * Sets the maximum number of allows entries per segment, returning the builder for method chaining.
     * <p>
     * The maximum entry count dictates when logs should roll over to new segments. As entries are written to a segment
     * of the log, if the entry count in that segment meets the configured maximum entry count, the log will create a
     * new segment and append new entries to that segment.
     * <p>
     * By default, the maximum entries per segment is {@code 1024 * 1024}.
     *
     * @param maxEntriesPerSegment The maximum number of entries allowed per segment.
     * @return The storage builder.
     * @throws IllegalArgumentException If the {@code maxEntriesPerSegment} not greater than the default max entries
     *     per segment
     * @deprecated since 3.0.2
     */
    @Deprecated
    public Builder<E> withMaxEntriesPerSegment(int maxEntriesPerSegment) {
      checkArgument(maxEntriesPerSegment > 0, "max entries per segment must be positive");
      checkArgument(maxEntriesPerSegment <= DEFAULT_MAX_ENTRIES_PER_SEGMENT,
          "max entries per segment cannot be greater than " + DEFAULT_MAX_ENTRIES_PER_SEGMENT);
      this.maxEntriesPerSegment = maxEntriesPerSegment;
      return this;
    }

    /**
     * Sets the journal index density.
     * <p>
     * The index density is the frequency at which the position of entries written to the journal will be recorded in an
     * in-memory index for faster seeking.
     *
     * @param indexDensity the index density
     * @return the journal builder
     * @throws IllegalArgumentException if the density is not between 0 and 1
     */
    public Builder<E> withIndexDensity(double indexDensity) {
      checkArgument(indexDensity > 0 && indexDensity < 1, "index density must be between 0 and 1");
      this.indexDensity = indexDensity;
      return this;
    }

    /**
     * Enables flushing buffers to disk when entries are committed to a segment, returning the builder for method
     * chaining.
     * <p>
     * When flush-on-commit is enabled, log entry buffers will be automatically flushed to disk each time an entry is
     * committed in a given segment.
     *
     * @return The storage builder.
     */
    public Builder<E> withFlushOnCommit() {
      return withFlushOnCommit(true);
    }

    /**
     * Sets whether to flush buffers to disk when entries are committed to a segment, returning the builder for method
     * chaining.
     * <p>
     * When flush-on-commit is enabled, log entry buffers will be automatically flushed to disk each time an entry is
     * committed in a given segment.
     *
     * @param flushOnCommit Whether to flush buffers to disk when entries are committed to a segment.
     * @return The storage builder.
     */
    public Builder<E> withFlushOnCommit(boolean flushOnCommit) {
      this.flushOnCommit = flushOnCommit;
      return this;
    }

    /**
     * Build the {@link SegmentedJournal}.
     *
     * @return A new {@link SegmentedJournal}.
     */
    public SegmentedJournal<E> build() {
      return new SegmentedJournal<>(
          name,
          storageLevel,
          directory,
          namespace,
          maxSegmentSize,
          maxEntrySize,
          maxEntriesPerSegment,
          indexDensity,
          flushOnCommit);
    }
  }
}
