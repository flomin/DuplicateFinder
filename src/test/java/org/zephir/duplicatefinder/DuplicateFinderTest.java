package org.zephir.duplicatefinder;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zephir.duplicatefinder.core.DuplicateFinderCore;

public class DuplicateFinderTest {
    @BeforeClass
    public static void setUpClass() throws Exception {}

    @AfterClass
    public static void tearDownClass() throws Exception {}

    @Before
    public void setUp() throws Exception {
        FileUtils.deleteDirectory(new File("C:\\wInd\\Boulot\\Java\\DuplicateFinder\\src\\test\\resources\\testFolder\\"));
//        FileUtils.copyDirectory(new File("C:\\wind\\Videos\\TT\\inthenameofkittens\\"),
        FileUtils.copyDirectory(new File("C:\\wInd\\Boulot\\Java\\DuplicateFinder\\src\\test\\resources\\source\\"),
                new File("C:\\wInd\\Boulot\\Java\\DuplicateFinder\\src\\test\\resources\\testFolder\\"));
    }

    @After
    public void tearDown() {}

    @Test
    public void testScalene() throws Exception {
        final DuplicateFinderCore core = new DuplicateFinderCore();
        core.setFolderToProcess(new File("C:\\wInd\\Boulot\\Java\\DuplicateFinder\\src\\test\\resources\\testFolder\\"));
        core.processFolderWithLIRE();
    }
}
