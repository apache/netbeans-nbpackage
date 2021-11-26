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
package org.apache.netbeans.nbpackage.deb;

import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;
import org.apache.commons.compress.harmony.archive.internal.nls.Messages;
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.Option;
import org.apache.netbeans.nbpackage.Packager;

/**
 * Packager for Linux DEB using dpkg-deb.
 */
public class DebPackager implements Packager {

    static final ResourceBundle MESSAGES
            = ResourceBundle.getBundle(DebPackager.class.getPackageName() + ".Messages");

    /**
     * Path to png icon (48x48) as required by AppDir / XDG specification.
     * Defaults to Apache NetBeans icon.
     */
    public static final Option<Path> DEB_ICON
            = Option.ofPath("package.deb.icon", "",
                    Messages.getString("option.icon.description"));

    /**
     * Path to svg icon. Will only be used if DEB_ICON is also set. Defaults to
     * Apache NetBeans icon.
     */
    public static final Option<Path> DEB_SVG
            = Option.ofPath("package.deb.svg-icon", "",
                    Messages.getString("option.svg.description"));

    /**
     * Name for the .desktop file (without suffix). Defaults to sanitized
     * version of package name.
     */
    public static final Option<String> DEB_DESKTOP_FILENAME
            = Option.ofString("package.deb.desktop-filename", "",
                    Messages.getString("option.desktopfilename.description"));

    /**
     * StartupWMClass to set in .desktop file.
     */
    public static final Option<String> DEB_WMCLASS
            = Option.ofString("package.deb.wmclass",
                    "${package.name}",
                    MESSAGES.getString("option.wmclass.description"));
    
    /**
     * Category (or categories) to set in .desktop file.
     */
    public static final Option<String> DEB_CATEGORY
            = Option.ofString("package.deb.category",
                    "Development;Java;IDE;",
                    MESSAGES.getString("option.category.description"));
    
    /**
     * Maintainer name and email for Debian Control file.
     */
    public static final Option<String> DEB_MAINTAINER
            = Option.ofString("package.deb.maintainer", "",
                    MESSAGES.getString("option.maintainer.description"));
    
    /**
     * Package description for Debian Control file.
     */
    public static final Option<String> DEB_DESCRIPTION
            = Option.ofString("package.deb.description", 
                    "Package of ${package.name} ${package.version}.",
                    MESSAGES.getString("option.description.description"));

    private static final List<Option<?>> DEB_OPTIONS
            = List.of(DEB_ICON, DEB_SVG, DEB_DESKTOP_FILENAME, DEB_WMCLASS,
                    DEB_CATEGORY, DEB_MAINTAINER, DEB_DESCRIPTION);

    @Override
    public Task createTask(ExecutionContext context) {
        return new DebTask(context);
    }

    @Override
    public String name() {
        return "linux-deb";
    }

    @Override
    public Stream<Option<?>> options() {
        return DEB_OPTIONS.stream();
    }

}
