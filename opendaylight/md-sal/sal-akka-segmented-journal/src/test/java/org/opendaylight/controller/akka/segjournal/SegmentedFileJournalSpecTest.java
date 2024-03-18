/*
 * Copyright (c) 2019 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import akka.persistence.japi.journal.JavaJournalSpec;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.runner.RunWith;
import org.scalatestplus.junit.JUnitRunner;

@RunWith(JUnitRunner.class)
public class SegmentedFileJournalSpecTest extends JavaJournalSpec {
    private static final long serialVersionUID = 1L;

    private static final File JOURNAL_DIR = new File("target/segmented-journal");

    public SegmentedFileJournalSpecTest() {
        super(ConfigFactory.load("SegmentedFileJournalTest.conf"));
    }

    @Override
    public void beforeAll() {
        FileUtils.deleteQuietly(JOURNAL_DIR);
        super.beforeAll();
    }

    @Override
    public void afterAll() {
        super.afterAll();
        FileUtils.deleteQuietly(JOURNAL_DIR);
    }
}
