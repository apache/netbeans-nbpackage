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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
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
                Files.delete(dir);
                return CONTINUE;
            }

        });
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

    private static void ensureWritable(Path path) throws IOException {
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
