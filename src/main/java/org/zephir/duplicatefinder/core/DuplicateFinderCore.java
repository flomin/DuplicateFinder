package org.zephir.duplicatefinder.core;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zephir.util.exception.CustomException;

public class DuplicateFinderCore implements DuplicateFinderConstants {
    private static Logger log = LoggerFactory.getLogger(DuplicateFinderCore.class);
    private File folderToProcess;

    private static final double SCORE_THRESHOLD = 5d;

    public DuplicateFinderCore() {}

    public void processFolderWithLIRE() throws CustomException {
        try {
            if (!folderToProcess.exists()) {
                log.error("processFolder() KO: folder must exist ('" + folderToProcess.getAbsolutePath() + "')");
                throw new CustomException("processFolder() KO: folder must exist ('" + folderToProcess.getAbsolutePath() + "')");
            }
            final String folderStr = "Folder to process: '" + folderToProcess.getAbsolutePath() + "'";
            log.info(StringUtils.repeat("_", folderStr.length()));
            log.info(folderStr);
            log.info(StringUtils.repeat("¯", folderStr.length()));

            // Index folder
            LIREHelper.parallelIndexFolder(folderToProcess, 6);
            Set<File> fileListToDelete = new TreeSet<File>();
            Set<File> fileListToIgnore = new TreeSet<File>();

            log.info("Listing files");
            // TODO: make descendIntoSubDirectories optional in parallelIndexFolder and here
            ArrayList<String> filesInFolder = net.semanticmetadata.lire.utils.FileUtils.getAllImages(folderToProcess, true);
//            File[] filesInFolder = folderToProcess.listFiles((FileFilter) FileFilterUtils.fileFileFilter());
            log.info(filesInFolder.size() + " files to search for duplicates in folder and subfolders");
            for (String filenameToCheck : filesInFolder) {
                File fileToCheck = new File(filenameToCheck);
                if (!fileListToIgnore.contains(fileToCheck)) {
                    if (TRACE) {
                        log.debug("Looking for duplicates from '" + fileToCheck.getName() + "'");
                    }
                    Collection<File> duplicateFileList = LIREHelper.searchImageForDuplicate(fileToCheck, SCORE_THRESHOLD);
                    if (duplicateFileList.size() > 1) {
                        File fileToKeep = chooseOneFromDuplicatedFiles(duplicateFileList, HighlanderMethod.SIZE_BIGGER, HighlanderMethod.FILENAME_BIGGER);
                        String duplicateFileListStr = duplicateFileList.stream().map(f -> f.getName()).collect(Collectors.joining(", "));
                        log.info("Keeping '" + fileToKeep.getName() + "' among duplicates: [" + duplicateFileListStr + "]");
                        fileListToIgnore.addAll(duplicateFileList);
                        duplicateFileList.remove(fileToKeep);
                        fileListToDelete.addAll(duplicateFileList);
                        if (TRACE) {
                            String fileListToDeleteStr = fileListToDelete.stream().map(f -> f.getName()).collect(Collectors.joining(", "));
                            log.info("fileListToDelete=[" + fileListToDeleteStr + "]");
                            String fileListToIgnoreStr = fileListToIgnore.stream().map(f -> f.getName()).collect(Collectors.joining(", "));
                            log.info("fileListToIgnore=[" + fileListToIgnoreStr + "]");
                        }
                    }
                }
            }

            log.info(fileListToDelete.size() + " duplicated file(s) detected among " + filesInFolder.size() + " file(s) in folder and subfolders");
            deleteFiles(fileListToDelete);
            LIREHelper.cleanIndexes();

        } catch (CustomException e) {
            throw e;
        } catch (Throwable e) {
            log.error("processFolder() KO: " + e, e);
            throw new CustomException("processFolder() KO: " + e, e);
        }
    }

    private static enum HighlanderMethod {
        FILENAME_SMALLER, FILENAME_BIGGER, SIZE_SMALLER, SIZE_BIGGER
    };

    private File chooseOneFromDuplicatedFiles(Collection<File> fileDuplicated, HighlanderMethod highlanderMethod1, HighlanderMethod highlanderMethod2) {
        // get the file with the shorter filename to be destroyed
        File fileToKeep = null;
        for (File f : fileDuplicated) {
            if (fileToKeep == null) {
                fileToKeep = f;
            } else {
                File newFileToKeep = selectFile(fileToKeep, f, highlanderMethod1);
                if (newFileToKeep == null) {
                    // first method couldn't choose
                    newFileToKeep = selectFile(fileToKeep, f, highlanderMethod2);
                }
                if (newFileToKeep == null) {
                    throw new IllegalStateException("Neither method could choose: " + highlanderMethod1.name() + " - " + highlanderMethod2.name());
                }
                fileToKeep = newFileToKeep;
            }
        }
        // return the list without the file to keep
        return fileToKeep;
    }

    private File selectFile(File f1, File f2, HighlanderMethod highlanderMethod) {
        File fileToKeep = null;
        switch (highlanderMethod) {
            case FILENAME_BIGGER :
                fileToKeep = f1.getName().length() > f2.getName().length() ? f1 : f2;
                break;
            case FILENAME_SMALLER :
                fileToKeep = f1.getName().length() < f2.getName().length() ? f1 : f2;
                break;
            case SIZE_BIGGER :
                if (f1.length() != f2.length()) {
                    // returns null if same
                    fileToKeep = f1.length() > f2.length() ? f1 : f2;
                }
                break;
            case SIZE_SMALLER :
                if (f1.length() != f2.length()) {
                    // returns null if same
                    fileToKeep = f1.length() < f2.length() ? f1 : f2;
                }
                break;
        }
        return fileToKeep;
    }

    private void deleteFiles(Collection<File> fileListToDelete) {
        int nbFilesDeleted = 0;
        for (File fileToBeDestroyed : fileListToDelete) {
            boolean destroyed = FileUtils.deleteQuietly(fileToBeDestroyed);
            if (destroyed) {
                nbFilesDeleted++;
                log.debug("File deleted '" + fileToBeDestroyed.getAbsolutePath() + "'");
            } else {
                log.warn("File could NOT be deleted '" + fileToBeDestroyed.getAbsolutePath() + "'");
            }
        }
        log.info(nbFilesDeleted + " duplicated file(s) deleted");
        log.info("");
    }

    public void processFolderWithSize() throws CustomException {
        try {
            if (!folderToProcess.exists()) {
                log.error("processFolder() KO: folder must exist ('" + folderToProcess.getAbsolutePath() + "')");
                throw new CustomException("processFolder() KO: folder must exist ('" + folderToProcess.getAbsolutePath() + "')");
            }
            final String folderStr = "Folder to process: '" + folderToProcess.getAbsolutePath() + "'";
            log.info(StringUtils.repeat("_", folderStr.length()));
            log.info(folderStr);
            log.info(StringUtils.repeat("¯", folderStr.length()));

            // Init variables
            final List<File> duplicatedFileList = new ArrayList<File>();

            log.info("Listing files");
            File[] filesInFolder = folderToProcess.listFiles((FileFilter) FileFilterUtils.fileFileFilter());
            log.info(filesInFolder.length + " files to search for duplicates");

            Collections.sort(Arrays.asList(filesInFolder), new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    int sizeCompare = ((Long) f1.length()).compareTo(f2.length());
                    try {
                        if (sizeCompare == 0) {
                            // same size -> compare md5
                            String f1md5 = new String(DigestUtils.md5(FileUtils.readFileToByteArray(f1)));
                            String f2md5 = new String(DigestUtils.md5(FileUtils.readFileToByteArray(f2)));
                            if (f1md5.equals(f2md5)) {
                                // duplicated files !
                                // get the file with the shorter filename to be destroyed
                                File fileToBeDetroyed = f1.getName().length() < f2.getName().length() ? f1 : f2;
                                log.debug("Files duplicated (same size and md5): '" + f1.getName() + "' == '" + f2.getName() + "' \t--> deleting '" + fileToBeDetroyed.getName() + "'");
                                duplicatedFileList.add(fileToBeDetroyed);
                            }
                            // else {
                            // log.debug("Files different: '" + f1.getName() + "' == '" + f2.getName() + "'");
                            // }
                        }
                    } catch (Exception e) {
                        log.error("checkFiles() KO during compare of '" + f1.getName() + "' and '" + f2.getName() + "': " + e, e);
                    }
                    return sizeCompare;
                }
            });

            log.info(duplicatedFileList.size() + " duplicated file(s) detected");
            int nbFilesDeleted = 0;
            for (File fileToBeDestroyed : duplicatedFileList) {
                boolean destroyed = FileUtils.deleteQuietly(fileToBeDestroyed);
                if (destroyed) {
                    nbFilesDeleted++;
                    log.debug("processFolder() file '" + fileToBeDestroyed.getAbsolutePath() + "' deleted");
                } else {
                    log.warn("processFolder() file '" + fileToBeDestroyed.getAbsolutePath() + "' couln't be deleted");
                }
            }
            log.info(nbFilesDeleted + " duplicated file(s) deleted");
            log.info("");

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("processFolder() KO: " + e, e);
            throw new CustomException("processFolder() KO: " + e, e);
        }
    }

    public File getFolderToProcess() {
        return folderToProcess;
    }

    public void setFolderToProcess(File folderToProcess) {
        this.folderToProcess = folderToProcess;
    }
}
