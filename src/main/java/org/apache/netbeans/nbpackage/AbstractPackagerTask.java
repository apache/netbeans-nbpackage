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
import java.util.List;
import java.util.Objects;

/**
 *
 */
public class AbstractPackagerTask implements Packager.Task {
    
    private final ExecutionContext context;
    
    protected AbstractPackagerTask(ExecutionContext context) {
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public void validateCreateImage() throws Exception {
        // no op
    }

    @Override
    public void validateCreateBuildFiles() throws Exception {
        // no op
    }
    
    @Override
    public void validateCreatePackage() throws Exception {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Path createImage() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public List<Path> createBuildFiles(Path image) throws Exception {
        return List.of();
    }

    @Override
    public Path createPackage(Path image, List<Path> buildFiles) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }



    

}
