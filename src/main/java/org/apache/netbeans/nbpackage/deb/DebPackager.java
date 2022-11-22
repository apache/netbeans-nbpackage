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
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.Option;
import org.apache.netbeans.nbpackage.Packager;
import org.apache.netbeans.nbpackage.Template;

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
    static final Option<Path> ICON_PATH
            = Option.ofPath("package.deb.icon", "",
                    MESSAGES.getString("option.icon.help"));

    /**
     * Path to svg icon. Will only be used if DEB_ICON is also set. Defaults to
     * Apache NetBeans icon.
     */
    static final Option<Path> SVG_ICON_PATH
            = Option.ofPath("package.deb.svg-icon", "",
                    MESSAGES.getString("option.svg.help"));

    /**
     * Name for the .desktop file (without suffix). Defaults to sanitized
     * version of package name.
     */
    static final Option<String> DESKTOP_FILENAME
            = Option.ofString("package.deb.desktop-filename", "",
                    MESSAGES.getString("option.desktopfilename.help"));

    /**
     * StartupWMClass to set in .desktop file.
     */
    static final Option<String> DESKTOP_WMCLASS
            = Option.ofString("package.deb.wmclass",
                    "${package.name}",
                    MESSAGES.getString("option.wmclass.help"));

    /**
     * Category (or categories) to set in .desktop file.
     */
    static final Option<String> DESKTOP_CATEGORY
            = Option.ofString("package.deb.category",
                    "Development;Java;IDE;",
                    MESSAGES.getString("option.category.help"));

    /**
     * Maintainer name and email for Debian Control file.
     */
    static final Option<String> DEB_MAINTAINER
            = Option.ofString("package.deb.maintainer", "",
                    MESSAGES.getString("option.maintainer.help"));

    /**
     * Package description for Debian Control file.
     */
    static final Option<String> DEB_DESCRIPTION
            = Option.ofString("package.deb.description",
                    "Package of ${package.name} ${package.version}.",
                    MESSAGES.getString("option.description.help"));

    /**
     * Optional path to custom DEB control template.
     */
    static final Option<Path> CONTROL_TEMPLATE_PATH
            = Option.ofPath("package.deb.control-template",
                    MESSAGES.getString("option.control_template.help"));

    /**
     * DEB control template.
     */
    static final Template CONTROL_TEMPLATE
            = Template.of(CONTROL_TEMPLATE_PATH, "deb.control.template",
                    () -> DebPackager.class.getResourceAsStream("deb.control.template"));

    /**
     * Optional path to custom .desktop template.
     */
    static final Option<Path> DESKTOP_TEMPLATE_PATH
            = Option.ofPath("package.deb.desktop-template",
                    MESSAGES.getString("option.desktop_template.help"));

    /**
     * Desktop file template.
     */
    static final Template DESKTOP_TEMPLATE
            = Template.of(DESKTOP_TEMPLATE_PATH, "deb.desktop.template",
                    () -> DebPackager.class.getResourceAsStream("deb.desktop.template"));

    /**
     * Optional path to custom launcher template.
     */
    static final Option<Path> LAUNCHER_TEMPLATE_PATH
            = Option.ofPath("package.deb.launcher-template",
                    MESSAGES.getString("option.launcher_template.help"));

    /**
     * Launcher script template.
     */
    static final Template LAUNCHER_TEMPLATE
            = Template.of(LAUNCHER_TEMPLATE_PATH, "deb.launcher.template",
                    () -> DebPackager.class.getResourceAsStream("deb.launcher.template"));

    private static final List<Option<?>> DEB_OPTIONS
            = List.of(ICON_PATH, SVG_ICON_PATH, DESKTOP_FILENAME, DESKTOP_WMCLASS,
                    DESKTOP_CATEGORY, DEB_MAINTAINER, DEB_DESCRIPTION,
                    CONTROL_TEMPLATE_PATH, DESKTOP_TEMPLATE_PATH, LAUNCHER_TEMPLATE_PATH);

    private static final List<Template> DEB_TEMPLATES
            = List.of(CONTROL_TEMPLATE, DESKTOP_TEMPLATE, LAUNCHER_TEMPLATE);

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

    @Override
    public Stream<Template> templates() {
        return DEB_TEMPLATES.stream();
    }

}
