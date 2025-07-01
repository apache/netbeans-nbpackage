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

import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ArchitectureTest {
    
    public ArchitectureTest() {
    }
    

    /**
     * Test of isSynonym method, of class Architecture.
     */
    @Test
    public void testIsSynonym() {
        assertTrue(Architecture.AARCH64.isSynonym("arm64"));
        assertTrue(Architecture.X86_64.isSynonym("amd64"));
        assertTrue(Architecture.AARCH64.isSynonym("AARCH64"));
        assertFalse(Architecture.X86_64.isSynonym("AArch64"));
    }

    /**
     * Test of detectFromPath method, of class Architecture.
     */
    @Test
    public void testDetectFromPath() {
        assertTrue(Architecture.detectFromPath(Path.of("parent", "jdk.tar.gz")).isEmpty());
        assertEquals(Architecture.X86_64,
                Architecture.detectFromPath(
                Path.of("parent", "jdk24.30.11-ca-jdk24.0.1-linux_x64.tar.gz")).orElseThrow());
        assertEquals(Architecture.AARCH64,
                Architecture.detectFromPath(
                Path.of("parent", "jdk24.30.11-ca-jdk24.0.1-macosx_aarch64.tar.gz")).orElseThrow());
        assertEquals(Architecture.X86_64,
                Architecture.detectFromPath(
                Path.of("aarch64", "jdk24.30.11-ca-jdk24.0.1-win_x64.zip")).orElseThrow());
    }
    
}
