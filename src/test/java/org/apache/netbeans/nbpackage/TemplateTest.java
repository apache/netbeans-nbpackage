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
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
public class TemplateTest {

    public TemplateTest() {
    }

    /**
     * Test of load method, of class Template.
     */
    @Test
    public void testLoad() throws Exception {
        Path tmpDir = Files.createTempDirectory("nbp-templates-");
        try {
            Option<Path> option1 = Option.ofPath("option1", "");
            Option<Path> option2 = Option.ofPath("option2", "");
            Template template1 = Template.of(option1, "Template 1",
                    () -> TemplateTest.class.getResourceAsStream("template1.template"));
            Template template2 = Template.of(option2, "Template 2",
                    () -> {
                        throw new AssertionError("Default source should not be called");
                    });
            Path override2 = Files.writeString(tmpDir.resolve("template2"),
                    "TEMPLATE TWO OVERRIDE", StandardOpenOption.CREATE_NEW);

            Configuration config = Configuration.builder()
                    .set(option2, override2.toAbsolutePath().toString())
                    .build();
            ExecutionContext ctxt = new ExecutionContext(null, null, config, null, false);
            String loaded1 = template1.load(ctxt);
            String loaded2 = template2.load(ctxt);
            assertEquals("TEMPLATE ONE", loaded1);
            assertEquals("TEMPLATE TWO OVERRIDE", loaded2);
        } finally {
            FileUtils.deleteFiles(tmpDir);
        }
    }

}
