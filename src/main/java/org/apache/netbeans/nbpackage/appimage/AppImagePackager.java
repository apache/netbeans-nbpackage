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
package org.apache.netbeans.nbpackage.appimage;

import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.Option;
import org.apache.netbeans.nbpackage.Packager;

/**
 * Packager for Linux AppImage, using appimagetool.
 */
public class AppImagePackager implements Packager {

    static final ResourceBundle MESSAGES
            = ResourceBundle.getBundle(AppImagePackager.class.getPackageName() + ".Messages");

    /**
     * Path to appimagetool executable.
     */
    static final Option<Path> APPIMAGE_TOOL
            = Option.ofPath("package.appimage.tool", "",
                    MESSAGES.getString("option.appimagetool.description"));

    /**
     * Path to png icon (48x48) as required by AppDir / XDG specification.
     */
    static final Option<Path> APPIMAGE_ICON
            = Option.ofPath("package.appimage.icon", "",
                    MESSAGES.getString("option.appimageicon.description"));

    /**
     * Category (or categories) to set in .desktop file.
     */
    static final Option<String> APPIMAGE_CATEGORY
            = Option.ofString("package.appimage.category",
                    "Development;Java;IDE;",
                    MESSAGES.getString("option.appimagecategory.description"));

    /**
     * Architecture of AppImage to create. Defaults to parsing from appimagetool
     * file name.
     */
    static final Option<String> APPIMAGE_ARCH
            = Option.ofString("package.appimage.arch",
                    "",
                    MESSAGES.getString("option.appimagearch.description"));

//    public static final Option<Path> APPIMAGE_SVG_ICON
//            = Option.ofPath("package.appimage.svg", "",
//                    MESSAGES.getString("option.appimagesvg.description"));
    private static final List<Option<?>> APPIMAGE_OPTIONS
            = List.of(APPIMAGE_TOOL, APPIMAGE_ICON, APPIMAGE_CATEGORY, APPIMAGE_ARCH);

    @Override
    public Task createTask(ExecutionContext context) {
        return new AppImageTask(context);
    }

    @Override
    public String name() {
        return "linux-appimage";
    }

    @Override
    public Stream<Option<?>> options() {
        return APPIMAGE_OPTIONS.stream();
    }

}
