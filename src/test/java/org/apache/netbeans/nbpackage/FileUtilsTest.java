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

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
public class FileUtilsTest {

    public FileUtilsTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of copyFiles method, of class FileUtils.
     */
    @Test
    public void testCopyFiles() throws Exception {
        Path src = Files.createTempDirectory("nbp-copy-src-");
        Path parent = src.resolve("foo").resolve("bar");
        Path file = parent.resolve("baz");
        Files.createDirectories(parent);
        Files.createFile(file);
        assertTrue(Files.isRegularFile(file));
        Path dst = Files.createTempDirectory("nbp-copy-dst-");
        FileUtils.copyFiles(src, dst);
        assertTrue(Files.isRegularFile(file));
        Path dstFile = dst.resolve(src.relativize(file));
        assertTrue(Files.isRegularFile(dstFile));
        FileUtils.deleteFiles(src);
        FileUtils.deleteFiles(dst);
    }

    /**
     * Test of moveFiles method, of class FileUtils.
     */
    @Test
    public void testMoveFiles() throws Exception {
        Path src = Files.createTempDirectory("nbp-move-src-");
        Path parent = src.resolve("foo").resolve("bar");
        Path file = parent.resolve("baz");
        Files.createDirectories(parent);
        Files.createFile(file);
        assertTrue(Files.isRegularFile(file));
        Path dst = Files.createTempDirectory("nbp-move-dst-");
        FileUtils.moveFiles(src, dst);
        assertFalse(Files.isRegularFile(file));
        Path dstFile = dst.resolve(src.relativize(file));
        assertTrue(Files.isRegularFile(dstFile));
        Files.delete(src);
        FileUtils.deleteFiles(dst);
    }

    /**
     * Test of deleteFiles method, of class FileUtils.
     */
    @Test
    public void testDeleteFiles() throws Exception {
        Path tmpDir = Files.createTempDirectory("nbp-delete-");
        Path parent = tmpDir.resolve("foo").resolve("bar");
        Path file = parent.resolve("baz");
        Files.createDirectories(parent);
        Files.createFile(file);
        assertTrue(Files.isRegularFile(file));
        FileUtils.deleteFiles(tmpDir.resolve("foo"));
        assertFalse(Files.exists(file));
        assertFalse(Files.exists(parent));
        Files.delete(tmpDir);
    }
    
    /**
     * Test of find method, of class FileUtils.
     * 
     * @throws Exception 
     */
    @Test
    public void testFind() throws Exception {
       Path tmpDir = Files.createTempDirectory("nbp-find-");
       try {
           List<Path> found = FileUtils.find(tmpDir, "**");
           assertTrue(found.isEmpty());
           Path dir = Files.createDirectory(tmpDir.resolve("dir"));
           Path file1 = Files.createFile(dir.resolve("file1.ext1"));
           Path file2 = Files.createFile(tmpDir.resolve("file2.ext2"));
           found = FileUtils.find(tmpDir, "*");
           assertEquals(3, found.size());
           found = FileUtils.find(tmpDir, "dir/*");
           assertEquals(List.of(file1), found);
           found = FileUtils.find(tmpDir, "*.{ext1,ext2}");
           assertEquals(List.of(file1, file2), found);
           found = FileUtils.find(tmpDir, "**.{ext1,ext2}");
           assertEquals(List.of(file1, file2), found);
           found = FileUtils.find(tmpDir, "**/*.{ext1,ext2}");
           assertEquals(List.of(file1), found);
           found = FileUtils.find(tmpDir, "*/file2.ext2");
           assertTrue(found.isEmpty());
       } finally {
           FileUtils.deleteFiles(tmpDir);
       }
    }
    

    /**
     * Test of findDirs method, of class FileUtils.
     */
    @Test
    public void testFindDirs() throws Exception {
        Path tmpDir = Files.createTempDirectory("nbp-find-dirs-");
        Path foo = tmpDir.resolve("foo");
        Path fooBin = foo.resolve("bin");
        Files.createDirectories(fooBin);
        Files.createFile(fooBin.resolve("launcher"));
        Path fooEtc = foo.resolve("etc");
        Files.createDirectories(fooEtc);
        Files.createFile(fooEtc.resolve("test.conf"));
        Path bar = tmpDir.resolve("bar");
        Path barBin = bar.resolve("bin");
        Files.createDirectories(barBin);
        Files.createFile(barBin.resolve("launcher"));

        List<Path> findDirs = FileUtils.findDirs(tmpDir, 1, "bin/launcher");

        assertEquals(2, findDirs.size());
        assertTrue(findDirs.contains(foo));
        assertTrue(findDirs.contains(bar));

        findDirs = FileUtils.findDirs(tmpDir, 1, "bin/launcher", "etc/*.conf");

        assertEquals(1, findDirs.size());
        assertTrue(findDirs.contains(foo));
        assertFalse(findDirs.contains(bar));

        FileUtils.deleteFiles(tmpDir);

    }
    
    @Test
    public void testProcessJarContents() throws Exception {
        Path tmpDir = Files.createTempDirectory("nbp-process-jar-");
        try {
            Path root = Files.createDirectory(tmpDir.resolve("root"));
            Path dir1 = Files.createDirectory(root.resolve("dir1"));
            Path dir2 = Files.createDirectory(root.resolve("dir2"));
            Path file1 = Files.writeString(dir1.resolve("file1"),
                    "File One",
                    StandardOpenOption.CREATE_NEW);
            Path file2 = Files.writeString(dir2.resolve("file2"),
                    "File Two",
                    StandardOpenOption.CREATE_NEW);
            Path jarFile = tmpDir.resolve("test.jar");
            FileUtils.createZipArchive(root, jarFile);
            
            int[] found = new int[]{0};
            boolean processed;
            
            processed = FileUtils.processJarContents(jarFile, "**/*", (file, path) -> {
                found[0] = found[0] + 1;
                Files.writeString(file, "GARBAGE");
                return false;
            });
            
            assertFalse(processed);
            assertEquals(2, found[0]);
            
            var jarURI = URI.create("jar:" + jarFile.toUri());
            
            try (var jarFS = FileSystems.newFileSystem(jarURI, Map.of())) {
                assertEquals("File One",
                        Files.readString(jarFS.getPath("dir1", "file1")));
                assertEquals("File Two",
                        Files.readString(jarFS.getPath("dir2", "file2")));
            }
            
            found[0] = 0;
            processed = FileUtils.processJarContents(jarFile, "/dir1/*", (file, path) -> {
                found[0] = found[0] + 1;
                assertEquals("file1", file.getFileName().toString());
                assertEquals("/dir1/file1", path);
                Files.writeString(file, "FILE ONE UPDATED");
                return true;
            });
            
            assertTrue(processed);
            assertEquals(1, found[0]);
            
            try (var jarFS = FileSystems.newFileSystem(jarURI, Map.of())) {
                assertEquals("FILE ONE UPDATED",
                        Files.readString(jarFS.getPath("dir1", "file1")));
                assertEquals("File Two",
                        Files.readString(jarFS.getPath("dir2", "file2")));
            }
        } finally {
            FileUtils.deleteFiles(tmpDir);
        }
        
    }

}
