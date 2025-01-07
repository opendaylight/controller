/*
 * Copyright (c) 2019 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import com.typesafe.config.ConfigFactory;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.apache.pekko.persistence.japi.journal.JavaJournalSpec;
import org.junit.runner.RunWith;
import org.scalatestplus.junit.JUnitRunner;

@RunWith(JUnitRunner.class)
public class SegmentedFileJournalSpecTest extends JavaJournalSpec {
    private static final long serialVersionUID = 1L;

    private static final Path JOURNAL_DIR = Path.of("target", "segmented-journal");

    public SegmentedFileJournalSpecTest() {
        super(ConfigFactory.load("SegmentedFileJournalTest.conf"));
    }

    @Override
    public void beforeAll() {
        FileUtils.deleteQuietly(JOURNAL_DIR.toFile());
        super.beforeAll();
    }

    @Override
    public void afterAll() {
        super.afterAll();
        FileUtils.deleteQuietly(JOURNAL_DIR.toFile());
    }
}
