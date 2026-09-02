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
package org.apache.netbeans.nbpackage.tar;

import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.Option;
import org.apache.netbeans.nbpackage.Packager;
import org.apache.netbeans.nbpackage.Template;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

/**
 * Packager for Linux embedded tar in script installer.
 */
public class TarScriptPackager implements Packager {

    static final ResourceBundle MESSAGES
            = ResourceBundle.getBundle(TarScriptPackager.class.getPackageName() + ".Messages");

    /**
     * Path to png icon (48x48) as required by AppDir / XDG specification.
     * Defaults to Apache NetBeans icon.
     */
    static final Option<Path> ICON_PATH
            = Option.ofPath("package.tar.icon",
                    MESSAGES.getString("option.icon.help"));

    /**
     * Path to svg icon. Defaults to Apache NetBeans icon.
     */
    static final Option<Path> SVG_ICON_PATH
            = Option.ofPath("package.tar.svg-icon",
                    MESSAGES.getString("option.svg.help"));

    /**
     * Name for the .desktop file (without suffix). Defaults to sanitized
     * version of package name.
     */
    static final Option<String> DESKTOP_FILENAME
            = Option.ofString("package.tar.desktop-filename",
                    MESSAGES.getString("option.desktopfilename.help"));

    /**
     * StartupWMClass to set in .desktop file.
     */
    static final Option<String> DESKTOP_WMCLASS
            = Option.ofString("package.tar.wmclass",
                    MESSAGES.getString("option.wmclass.default"),
                    MESSAGES.getString("option.wmclass.help"));

    /**
     * Category (or categories) to set in .desktop file.
     */
    static final Option<String> DESKTOP_CATEGORY
            = Option.ofString("package.tar.category",
                    MESSAGES.getString("option.category.default"),
                    MESSAGES.getString("option.category.help"));

    /**
     * Optional path to custom .desktop template.
     */
    static final Option<Path> DESKTOP_TEMPLATE_PATH
            = Option.ofPath("package.tar.desktop-template",
                    MESSAGES.getString("option.desktop_template.help"));

    /**
     * Desktop file template.
     */
    static final Template DESKTOP_TEMPLATE
            = Template.of(DESKTOP_TEMPLATE_PATH, "tar.desktop.template",
                    () -> TarScriptPackager.class.getResourceAsStream("tar.desktop.template"));

    /**
     * Optional path to custom launcher template.
     */
    static final Option<Path> LAUNCHER_TEMPLATE_PATH
            = Option.ofPath("package.tar.launcher-template",
                    MESSAGES.getString("option.launcher_template.help"));

    /**
     * Launcher script template.
     */
    static final Template LAUNCHER_TEMPLATE
            = Template.of(LAUNCHER_TEMPLATE_PATH, "tar.launcher.template",
                    () -> TarScriptPackager.class.getResourceAsStream("tar.launcher.template"));

    /**
     * Path to alternative tar script template.
     */
    static final Option<Path> TAR_SCRIPT_TEMPLATE_PATH
            = Option.ofPath("package.tar.template", "",
                    MESSAGES.getString("option.template.help"));

    /**
     * Shell file template.
     */
    static final Template TAR_SCRIPT_TEMPLATE
            = Template.of(TAR_SCRIPT_TEMPLATE_PATH, "tar.script.template",
                    () -> TarScriptPackager.class.getResourceAsStream("tar.script.template"));

    private static final List<Option<?>> TAR_SCRIPT_OPTIONS
            = List.of(ICON_PATH, SVG_ICON_PATH, DESKTOP_FILENAME, DESKTOP_WMCLASS,
                    DESKTOP_CATEGORY, DESKTOP_TEMPLATE_PATH, LAUNCHER_TEMPLATE_PATH);

    private static final List<Template> TAR_SCRIPT_TEMPLATES
            = List.of(TAR_SCRIPT_TEMPLATE, DESKTOP_TEMPLATE, LAUNCHER_TEMPLATE);

    @Override
    public Task createTask(ExecutionContext context) {
        return new TarScriptPackageTask(context);
    }

    @Override
    public String name() {
        return "linux-tar-script";
    }

    @Override
    public Stream<Option<?>> options() {
        return TAR_SCRIPT_OPTIONS.stream();
    }

    @Override
    public Stream<Template> templates() {
        return TAR_SCRIPT_TEMPLATES.stream();
    }
}
