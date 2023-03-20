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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Various tests for {@link AbstractPackagerTask}.
 */
public class AbstractPackagerTaskTest {

    private static Path tmpDir;
    private static Path input;
    private static Path runtime;

    public AbstractPackagerTaskTest() {
    }

    @BeforeAll
    public static void setUpClass() throws IOException {
        tmpDir = Files.createTempDirectory("nbp-task-tests-");
        Path appRoot = Files.createDirectory(tmpDir.resolve("appDir"));
        Path appBin = Files.createDirectory(appRoot.resolve("bin"));
        Path appEtc = Files.createDirectory(appRoot.resolve("etc"));
        Path platform = Files.createDirectory(appRoot.resolve("platform"));
        Files.createFile(appBin.resolve("app"));
        Files.createFile(appEtc.resolve("app.conf"));
        Files.createFile(platform.resolve("module"));
        input = tmpDir.resolve("app.zip");
        FileUtils.createZipArchive(appRoot, input);
        Path jdkRoot = Files.createDirectory(tmpDir.resolve("jdkDir"));
        Path jdkBin = Files.createDirectory(jdkRoot.resolve("bin"));
        Files.createFile(jdkBin.resolve("java"));
        runtime = tmpDir.resolve("runtime.zip");
        FileUtils.createZipArchive(jdkRoot, runtime);
    }

    @AfterAll
    public static void tearDownClass() throws IOException {
        if (tmpDir != null) {
            FileUtils.deleteFiles(tmpDir);
            tmpDir = null;
        }
    }

    /**
     * Test extraction of the input zip to create a base image.
     */
    @Test
    public void testCreateBaseImage() throws Exception {
        Configuration config = Configuration.builder()
                .set(NBPackage.PACKAGE_NAME, "App")
                .build();
        Path output = Files.createDirectory(tmpDir.resolve("baseOnlyOutput"));
        class TestTask extends AbstractPackagerTask {

            public TestTask(ExecutionContext context) {
                super(context);
            }

            @Override
            protected Path buildPackage(Path image) throws Exception {
                throw new IllegalStateException();
            }

            @Override
            protected void customizeImage(Path image) throws Exception {
            }

        }
        ExecutionContext context = new ExecutionContext(
                new TestPackager("Test Base Only", TestTask::new),
                input,
                config,
                output,
                true);
        Path image = context.execute();
        assertEquals(output, image.getParent());
        assertTrue(Files.exists(image.resolve("bin").resolve("app")));
        assertTrue(Files.exists(image.resolve("etc").resolve("app.conf")));
        assertTrue(Files.exists(image.resolve("platform").resolve("module")));
        assertFalse(Files.exists(image.resolve("jdk")));
    }
    
    /**
     * Test extraction of the input zip and runtime zip to create a base image.
     */
    @Test
    public void testCreateBaseImageWithRuntime() throws Exception {
        Configuration config = Configuration.builder()
                .set(NBPackage.PACKAGE_NAME, "App")
                .set(NBPackage.PACKAGE_RUNTIME, runtime.toString())
                .build();
        Path output = Files.createDirectory(tmpDir.resolve("baseAndRuntimeOutput"));
        class TestTask extends AbstractPackagerTask {

            public TestTask(ExecutionContext context) {
                super(context);
            }

            @Override
            protected Path buildPackage(Path image) throws Exception {
                throw new IllegalStateException();
            }

            @Override
            protected void customizeImage(Path image) throws Exception {
            }

        }
        ExecutionContext context = new ExecutionContext(
                new TestPackager("Test Base with Runtime", TestTask::new),
                input,
                config,
                output,
                true);
        Path image = context.execute();
        assertEquals(output, image.getParent());
        assertTrue(Files.exists(image.resolve("bin").resolve("app")));
        assertTrue(Files.exists(image.resolve("etc").resolve("app.conf")));
        assertTrue(Files.exists(image.resolve("platform").resolve("module")));
        assertTrue(Files.exists(image.resolve("jdk").resolve("bin").resolve("java")));
    }

    private static class TestPackager implements Packager {

        private final String name;
        private final Function<ExecutionContext, Task> taskCreator;

        private TestPackager(String name, Function<ExecutionContext, Task> taskCreator) {
            this.name = name;
            this.taskCreator = taskCreator;
        }

        @Override
        public Task createTask(ExecutionContext context) {
            return taskCreator.apply(context);
        }

        @Override
        public String name() {
            return name;
        }

    }

}
