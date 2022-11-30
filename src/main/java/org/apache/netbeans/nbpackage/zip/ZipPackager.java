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
package org.apache.netbeans.nbpackage.zip;

import java.util.ResourceBundle;
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.Packager;

/**
 * Basic zip packager. Mainly useful for testing and debugging purposes. Can
 * also zip platform application and runtime together (less useful for IDE until
 * NetBeans launcher supports relative jdkhome).
 */
public class ZipPackager implements Packager {
    
    static final ResourceBundle MESSAGES
            = ResourceBundle.getBundle(ZipPackager.class.getPackageName() + ".Messages");

    @Override
    public Task createTask(ExecutionContext context) {
        return new ZipPackageTask(context);
    }

    @Override
    public String name() {
        return "zip";
    }

}
