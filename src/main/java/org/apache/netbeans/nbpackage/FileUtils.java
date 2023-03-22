/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.netbeans.nbpackage;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.compressors.CompressorException;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * A range of useful file utility functions for packagers.
 */
public class FileUtils {

    private FileUtils() {
        // static utilities
    }

    /**
     * Copy files from the source directory to the destination directory
     * recursively.
     *
     * @param src source directory
     * @param dst destination directory
     * @throws IOException
     */
    public static void copyFiles(Path src, Path dst) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Path targetDir = dst.resolve(src.relativize(dir));
                try {
                    Files.copy(dir, targetDir);
                    ensureWritable(targetDir);
                } catch (FileAlreadyExistsException ex) {
                    if (!Files.isDirectory(targetDir)) {
                        throw ex;
                    }
                }
                return CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Path targetFile = dst.resolve(src.relativize(file));
                Files.copy(file, targetFile,
                        StandardCopyOption.COPY_ATTRIBUTES);
                ensureWritable(targetFile);
                return CONTINUE;
            }

        });
    }

    /**
     * Create a zip file of the provided directory, maintaining file attributes.
     * The destination file must not already exist.
     *
     * @param directory directory to zip
     * @param destination destination file (must not exist)
     * @throws IOException
     */
    public static void createZipArchive(Path directory, Path destination) throws IOException {
        if (Files.exists(destination)) {
            throw new IOException(destination.toString());
        }
        try {
            ArchiveUtils.createArchive(ArchiveUtils.ArchiveType.ZIP, directory, destination);
        } catch (ArchiveException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Extract an archive into the given destination directory, maintaining file
     * attributes where possible. The destination directory must already exist.
     * Supports zip, tar and tar.gz archives. Limited support for other archive
     * types supported by Apache Commons Compress, but file attributes may not
     * be supported.
     *
     * @param archive archive file
     * @param destination destination directory (must exist)
     * @throws IOException
     */
    public static void extractArchive(Path archive, Path destination) throws IOException {
        if (!Files.isDirectory(destination)) {
            throw new IOException(destination.toString());
        }
        try {
            ArchiveUtils.extractArchive(archive, destination);
        } catch (CompressorException | ArchiveException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Move files from the source directory to the destination directory
     * recursively.
     *
     * @param src source directory
     * @param dst destination directory
     * @throws IOException
     */
    public static void moveFiles(Path src, Path dst) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                if (dir.equals(src)) {
                    return CONTINUE;
                }
                Path targetDir = dst.resolve(src.relativize(dir));
                try {
                    Files.copy(dir, targetDir);
                    ensureWritable(targetDir);
                } catch (FileAlreadyExistsException ex) {
                    if (!Files.isDirectory(targetDir)) {
                        throw ex;
                    }
                }
                return CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Path targetFile = dst.resolve(src.relativize(file));
                Files.move(file, targetFile);
                ensureWritable(targetFile);
                return CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                if (dir.equals(src)) {
                    return CONTINUE;
                }
                Files.delete(dir);
                return CONTINUE;
            }

        });
    }

    /**
     * Recursively delete a directory and all files inside it.
     *
     * @param dir directory to delete
     * @throws IOException
     */
    public static void deleteFiles(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

        });
    }

    /**
     * Find all files that match the provided glob pattern within a directory.
     * Files will be included if their relative path within the directory, or
     * their filename, match the pattern.
     *
     * @param searchDir directory to search within
     * @param pattern pattern to search for
     * @return list of files that match pattern
     * @throws IOException
     */
    public static List<Path> find(Path searchDir, String pattern) throws IOException {
        var matcher = searchDir.getFileSystem().getPathMatcher("glob:" + pattern);
        try (var stream = Files.find(searchDir, Integer.MAX_VALUE, (file, attr)
                -> !file.equals(searchDir) && 
                        (matcher.matches(searchDir.relativize(file)) ||
                                matcher.matches(file.getFileName()))
        )) {
            return stream.collect(Collectors.toList());
        }
    }

    /**
     * Find the directories in the given search directory that contains files
     * that match the given glob patterns. This might include the search
     * directory itself. eg. to find NetBeans or a RCP application in a search
     * directory you might pass <code>"bin/*", "etc/*.conf"</code>.
     *
     * @param searchDir search directory
     * @param searchDepth search depth
     * @param patterns glob patterns
     * @return list of matching directories
     * @throws IOException
     */
    public static List<Path> findDirs(Path searchDir, int searchDepth, String... patterns) throws IOException {
        var matchers = Stream.of(patterns)
                .map(p -> FileSystems.getDefault().getPathMatcher("glob:" + p))
                .collect(Collectors.toList());
        var intDepth = Stream.of(patterns)
                .map(Path::of)
                .mapToInt(Path::getNameCount)
                .max().orElse(1);
        try (var stream = Files.find(searchDir, searchDepth, (intPath, attr) -> {
            return matchers.stream().map(m -> {
                try (var files = Files.walk(intPath, intDepth)) {
                    return files.map(file -> intPath.relativize(file))
                            .anyMatch(m::matches);
                } catch (IOException ex) {
                    return false;
                }
            }).allMatch(v -> v);
        })) {
            return List.copyOf(stream.collect(Collectors.toList()));
        }
    }

    /**
     * Process files inside a JAR file. This can be used, for example, to sign
     * native executables inside a JAR. Files whose path inside the JAR matches
     * the given pattern will be extracted into a temporary directory and passed
     * to the provided processor. If the processor returns true the file will be
     * updated inside the JAR.
     *
     * @param jarFile JAR file
     * @param pattern glob pattern to match paths for processing
     * @param processor process files
     * @return true if the JAR contents have been updated
     * @throws IOException
     */
    public static boolean processJarContents(Path jarFile, String pattern, JarProcessor processor)
            throws IOException {
        URI jarURI = URI.create("jar:" + jarFile.toUri());
        boolean updated = false;
        try (var jarFS = FileSystems.newFileSystem(jarURI, Map.of())) {
            PathMatcher matcher = jarFS.getPathMatcher("glob:" + pattern);
            List<Path> filesToProcess;
            try (var jarStream = Files.walk(jarFS.getPath("/"))) {
                filesToProcess = jarStream
                        .filter(file -> Files.isRegularFile(file))
                        .filter(matcher::matches)
                        .collect(Collectors.toList());
            }
            if (filesToProcess.isEmpty()) {
                return false;
            }
            Path tmpDir = Files.createTempDirectory("nbpackage-jar-");
            try {
                for (Path file : filesToProcess) {
                    Path tmp = null;
                    try {
                        tmp = Files.copy(file, tmpDir.resolve(file.getFileName().toString()));
                        boolean processed = processor.processFile(tmp, file.toString());
                        if (processed) {
                            Files.copy(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                            updated = true;
                        }
                    } finally {
                        Files.deleteIfExists(tmp);
                    }
                }

            } finally {
                Files.delete(tmpDir);
            }
        }
        return updated;
    }

    /**
     * Processor for updating files inside a JAR. For use with
     * {@link FileUtils#processJarContents(java.nio.file.Path, java.lang.String, org.apache.netbeans.nbpackage.FileUtils.JarProcessor)}.
     */
    @FunctionalInterface
    public static interface JarProcessor {

        /**
         * Process file from JAR.
         *
         * @param tmpFile path to temporary extracted file
         * @param jarPath full path inside JAR
         * @return true to update the file in the JAR
         * @throws IOException
         */
        public boolean processFile(Path tmpFile, String jarPath) throws IOException;

    }

    /**
     * Remove the extension from the given file name, if one exists. Simply
     * removes the last dot and remaining characters.
     *
     * @param filename filename
     * @return filename without extension
     */
    public static String removeExtension(String filename) {
        int lastDot = filename.lastIndexOf(".");
        if (lastDot > 0) {
            return filename.substring(0, lastDot);
        } else {
            return filename;
        }
    }

    static void ensureWritable(Path path) throws IOException {
        if (Files.isWritable(path) || Files.isSymbolicLink(path)) {
            return;
        }
        var posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (posix != null) {
            var perms = posix.readAttributes().permissions();
            perms.add(PosixFilePermission.OWNER_WRITE);
            posix.setPermissions(perms);
            return;
        }
        var dos = Files.getFileAttributeView(path, DosFileAttributeView.class);
        if (dos != null) {
            dos.setReadOnly(false);
        }

    }

}
