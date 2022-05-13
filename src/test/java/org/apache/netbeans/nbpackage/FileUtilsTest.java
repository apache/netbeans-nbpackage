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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

}
