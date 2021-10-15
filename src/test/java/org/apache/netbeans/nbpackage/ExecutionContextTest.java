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
public class ExecutionContextTest {

    public ExecutionContextTest() {
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
     * Test of replaceTokens method, of class ExecutionContext.
     */
    @Test
    public void testReplaceTokens_String_UnaryOperator() {
        Configuration config = Configuration.builder().
                set(NBPackage.PACKAGE_NAME, "Apache NetBeans")
                .build();
        ExecutionContext ctxt = new ExecutionContext(null, null, config, null, false);
        String text = "The package name is ${package.name}.";
        String processed = ctxt.replaceTokens(text);
        assertEquals("The package name is Apache NetBeans.", processed);

        config = Configuration.builder()
                .set(NBPackage.PACKAGE_NAME, "Apache NetBeans")
                .set(NBPackage.PACKAGE_VERSION, "101.1")
                .build();
        ctxt = new ExecutionContext(null, null, config, null, false);
        
        text = "${package.name}\nat version : ${package.version}.";
        processed = ctxt.replaceTokens(text);
        assertEquals("Apache NetBeans\nat version : 101.1.", processed);
        
        text = "${package.name} execution token ${EXEC}.";
        processed = ctxt.replaceTokens(text);
        assertEquals("Apache NetBeans execution token ${EXEC}.", processed);
    }

//    /**
//     * Test of exec method, of class ExecutionContext.
//     */
//    @Test
//    public void testExec() throws Exception {
//        var config = Configuration.builder().verbose().build();
//        var ctxt = new ExecutionContext(null, null, config, null, false);
//        ctxt.exec(ctxt.splitCommandLine("ls \"-al\"  "));
//    }
//
//    /**
//     * Test of execAndGetOutput method, of class ExecutionContext.
//     */
//    @Test
//    public void testExecAndGetOutput() throws Exception {
//        var config = Configuration.builder().verbose().build();
//        var ctxt = new ExecutionContext(null, null, config, null, false);
//        System.out.println(ctxt.execAndGetOutput(List.of("ls", "-al")));
//    }

}
