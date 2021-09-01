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
import java.util.stream.Stream;

/**
 * API to be implemented by all packagers.
 */
public interface Packager {

    /**
     * Name of the packager. Used as an ID to look up implementations.
     *
     * @return packager name
     */
    public String name();

    /**
     * Create a new packaging task for the given context. The context provides
     * access to utilities and configuration.
     *
     * @param context execution context
     * @return task
     */
    public Task createTask(ExecutionContext context);

    /**
     * A stream of packager-specific options provided by this packager. The
     * default implementation provides an empty stream.
     *
     * @return stream of packager-specific options
     */
    public default Stream<Option<?>> options() {
        return Stream.empty();
    }

    /**
     * Task API. A task only supports a single execution. Not all stages of the
     * task may be executed - eg. when only creating a package image or creating
     * a package from an image. The validation methods for all required stages
     * will be executed before any of the stages themselves. Validation methods
     * do quick checks for common problems before starting execution - they do
     * not guarantee that execution will succeed.
     */
    public static interface Task {

        /**
         * Ensure basic configuration is in place to create a package image with
         * this packager.
         *
         * @throws Exception on validation failure
         */
        public void validateCreateImage() throws Exception;

        /**
         * Ensure basic configuration is in place to create any additional build
         * files for the package image. The default implementation does nothing
         * as the majority of packagers will not need this step.
         *
         * @throws Exception on validation failure
         */
        public default void validateCreateBuildFiles() throws Exception {
            // no op
        }

        /**
         * Ensure basic configuration is in place to create a package with this
         * packager - eg. native tooling is available or required options set.
         *
         * @throws Exception on validation failure
         */
        public void validateCreatePackage() throws Exception;

        /**
         * Create the package image. This will usually be a directory with the
         * specific layout and files required for passing in to the packaging
         * task.
         *
         * @param input archive or directory containing application to package
         * @return path to image
         * @throws Exception on execution failure
         */
        public Path createImage(Path input) throws Exception;

        /**
         * Create any additional files external to the image required by the
         * packaging implementation. The default implementation returns an
         * empty, immutable list, as the majority of packagers will not need
         * this step.
         *
         * @param image image created by {@link #createImage()}
         * @return list of external files
         * @throws Exception on execution failure
         */
        public default List<Path> createBuildFiles(Path image) throws Exception {
            return List.of();
        }

        /**
         * Create a package from the image and additional files created by
         * {@link #createImage()} and
         * {@link #createBuildFiles(java.nio.file.Path)}.
         *
         * @param image image created by {@link #createImage()}
         * @param buildFiles addition build files created by {@link #createBuildFiles(java.nio.file.Path)}
         * @return path to created package
         * @throws Exception on execution failure
         */
        public Path createPackage(Path image, List<Path> buildFiles) throws Exception;

    }

}
