package org.zephir.duplicatefinder.core;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.semanticmetadata.lire.builders.DocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.features.global.AutoColorCorrelogram;
import net.semanticmetadata.lire.imageanalysis.features.global.CEDD;
import net.semanticmetadata.lire.imageanalysis.features.global.FCTH;
import net.semanticmetadata.lire.indexers.parallel.ParallelIndexer;
import net.semanticmetadata.lire.searchers.GenericFastImageSearcher;
import net.semanticmetadata.lire.searchers.ImageSearchHits;
import net.semanticmetadata.lire.searchers.ImageSearcher;

public class LIREHelper {
    private static Logger log = LoggerFactory.getLogger(LIREHelper.class);

    private static IndexReader indexReader;

    public static void parallelIndexFolder(File folderToIndex, int nbThreads) throws Throwable {
        if (!folderToIndex.exists() || !folderToIndex.isDirectory()) {
            throw new FileNotFoundException(folderToIndex.getAbsolutePath());
        }

        closeIndexReader();

        // use ParallelIndexer to index all photos from f into "index" ... use 6 threads (actually 7 with the I/O thread).
        final ParallelIndexer indexer = new ParallelIndexer(nbThreads, "indexPath", folderToIndex.getAbsolutePath());
        // use this to add you preferred builders. For now we go for CEDD, FCTH and AutoColorCorrelogram
        indexer.addExtractor(CEDD.class);
        indexer.addExtractor(FCTH.class);
        indexer.addExtractor(AutoColorCorrelogram.class);
        new Thread(() -> {
            try {
                boolean indexStarted = false;
                while (!indexer.hasEnded()) {
                    Thread.sleep(2000);
                    if (!indexer.hasEnded()) {
                        indexStarted = indexStarted || indexer.getPercentageDone() < 1;
                        if (indexStarted) {
                            log.info("Indexing " + (int) (indexer.getPercentageDone() * 100) + "%");
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Indexer percent thread KO: " + e, e);
            }
        }).start();
        log.info("Indexing images in folder '" + folderToIndex.getAbsolutePath() + "'");
        indexer.run();

        log.info("Finished indexing folder '" + folderToIndex.getAbsolutePath() + "'");
    }

    // https://github.com/dermotte/LIRE/blob/master/src/main/docs/developer-docs/docs/searchindex.md
    public static Collection<File> searchImageForDuplicate(File imageToSearch, double scoreThreshold) throws Exception {
        if (!imageToSearch.exists() || !imageToSearch.isFile()) {
            throw new FileNotFoundException(imageToSearch.getAbsolutePath());
        }
        BufferedImage img = null;
        try {
            img = ImageIO.read(imageToSearch);
        } catch (Exception e) {
            log.error("File isn't an image '" + imageToSearch.getName() + "'");
            throw new Exception("File isn't an image '" + imageToSearch.getName() + "'");
        }

        if (indexReader == null) {
            indexReader = DirectoryReader.open(FSDirectory.open(Paths.get("indexPath")));
        }
        ImageSearcher searcher = new GenericFastImageSearcher(30, CEDD.class);

        ImageSearchHits hits = searcher.search(img, indexReader);
        Set<File> duplicateFiles = new TreeSet<File>();
        duplicateFiles.add(imageToSearch);
        for (int i = 0; i < hits.length(); i++) {
            String fileName = indexReader.document(hits.documentID(i)).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0];
            double score = hits.score(i);
            if (score <= scoreThreshold) {
                File f = new File(fileName);
                if (DuplicateFinderConstants.TRACE) {
                    log.debug("File duplicate with score=" + score + ": \t'" + f.getName() + "'");
                }
                duplicateFiles.add(f);
            } else if (DuplicateFinderConstants.TRACE) {
                File f = new File(fileName);
                log.debug("File NOT duplicate with score=" + score + ": \t'" + f.getName() + "'");
            }
        }
        return duplicateFiles;
    }

    private static void closeIndexReader() throws Exception {
        if (indexReader != null) {
            indexReader.close();
            indexReader = null;
        }
    }

    public static void cleanIndexes() throws Exception {
        closeIndexReader();
        int nbFolderCleaned = 0;
        for (String folderName : Arrays.asList("index", "index.config", "indexPath", "indexPath.config")) {
            File folder = new File(folderName);
            if (folder.exists()) {
                try {
                    FileUtils.deleteDirectory(folder);
                    nbFolderCleaned++;
                } catch (Exception e) {
                    log.warn("Can't delete index folder '" + folder.getAbsolutePath() + "': " + e);
                }
            }
        }
        log.info(nbFolderCleaned + " index folders cleaned");
    }
}
