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
package org.apache.netbeans.nbpackage.rpm;

import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;
import org.apache.netbeans.nbpackage.ExecutionContext;
import org.apache.netbeans.nbpackage.Option;
import org.apache.netbeans.nbpackage.Packager;
import org.apache.netbeans.nbpackage.Template;

/**
 * Packager for Linux RPM using rpmbuild.
 */
public class RpmPackager implements Packager {

    static final ResourceBundle MESSAGES
            = ResourceBundle.getBundle(RpmPackager.class.getPackageName() + ".Messages");

    /**
     * Path to png icon (48x48) as required by AppDir / XDG specification.
     * Defaults to Apache NetBeans icon.
     */
    static final Option<Path> ICON_PATH
            = Option.ofPath("package.rpm.icon", "",
                    MESSAGES.getString("option.icon.description"));

    /**
     * Path to svg icon. Will only be used if RPM_ICON is also set. Defaults to
     * Apache NetBeans icon.
     */
    static final Option<Path> SVG_ICON_PATH
            = Option.ofPath("package.rpm.svg-icon", "",
                    MESSAGES.getString("option.svg.description"));

    /**
     * Name for the .desktop file (without suffix). Defaults to sanitized
     * version of package name.
     */
    static final Option<String> DESKTOP_FILENAME
            = Option.ofString("package.rpm.desktop-filename", "",
                    MESSAGES.getString("option.desktopfilename.description"));

    /**
     * StartupWMClass to set in .desktop file.
     */
    static final Option<String> DESKTOP_WMCLASS
            = Option.ofString("package.rpm.wmclass",
                    "${package.name}",
                    MESSAGES.getString("option.wmclass.description"));

    /**
     * Category (or categories) to set in .desktop file.
     */
    static final Option<String> DESKTOP_CATEGORY
            = Option.ofString("package.rpm.category",
                    "Development;Java;IDE;",
                    MESSAGES.getString("option.category.description"));

    /**
     * Maintainer name and email for the RPM spec.
     */
    static final Option<String> RPM_MAINTAINER
            = Option.ofString("package.rpm.maintainer", "",
                    MESSAGES.getString("option.maintainer.description"));
    
    /**
     * Software license.
     */
    static final Option<String> RPM_LICENSE
            = Option.ofString("package.rpm.license", "",
                    MESSAGES.getString("option.license.description"));

    /**
     * Package summary for the RPM spec.
     */
    static final Option<String> RPM_SUMMARY
            = Option.ofString("package.rpm.summary",
                    "Package of ${package.name} ${package.version}.",
                    MESSAGES.getString("option.summary.description"));
    
    /**
     * Package description for the RPM spec.
     */
    static final Option<String> RPM_DESCRIPTION
            = Option.ofString("package.rpm.description",
                    "Package of ${package.name} ${package.version}.",
                    MESSAGES.getString("option.description.description"));

    /**
     * Optional path to custom RPM spec template.
     */
    static final Option<Path> SPEC_TEMPLATE_PATH
            = Option.ofPath("package.rpm.spec-template",
                    MESSAGES.getString("option.control_template.description"));

    /**
     * RPM spec template.
     */
    static final Template SPEC_TEMPLATE
            = Template.of(SPEC_TEMPLATE_PATH, "rpm.spec.template",
                    () -> RpmPackager.class.getResourceAsStream("rpm.spec.template"));

    /**
     * Optional path to custom .desktop template.
     */
    static final Option<Path> DESKTOP_TEMPLATE_PATH
            = Option.ofPath("package.rpm.desktop-template",
                    MESSAGES.getString("option.desktop_template.description"));

    /**
     * Desktop file template.
     */
    static final Template DESKTOP_TEMPLATE
            = Template.of(DESKTOP_TEMPLATE_PATH, "rpm.desktop.template",
                    () -> RpmPackager.class.getResourceAsStream("rpm.desktop.template"));

    /**
     * Optional path to custom launcher template.
     */
    static final Option<Path> LAUNCHER_TEMPLATE_PATH
            = Option.ofPath("package.rpm.launcher-template",
                    MESSAGES.getString("option.launcher_template.description"));

    /**
     * Launcher script template.
     */
    static final Template LAUNCHER_TEMPLATE
            = Template.of(LAUNCHER_TEMPLATE_PATH, "rpm.launcher.template",
                    () -> RpmPackager.class.getResourceAsStream("rpm.launcher.template"));

    private static final List<Option<?>> RPM_OPTIONS
            = List.of(ICON_PATH, SVG_ICON_PATH, DESKTOP_FILENAME, DESKTOP_WMCLASS,
                    DESKTOP_CATEGORY, RPM_MAINTAINER, RPM_LICENSE, RPM_DESCRIPTION,
                    SPEC_TEMPLATE_PATH, DESKTOP_TEMPLATE_PATH, LAUNCHER_TEMPLATE_PATH);

    private static final List<Template> RPM_TEMPLATES
            = List.of(SPEC_TEMPLATE, DESKTOP_TEMPLATE, LAUNCHER_TEMPLATE);

    @Override
    public Task createTask(ExecutionContext context) {
        return new RpmTask(context);
    }

    @Override
    public String name() {
        return "linux-rpm";
    }

    @Override
    public Stream<Option<?>> options() {
        return RPM_OPTIONS.stream();
    }

    @Override
    public Stream<Template> templates() {
        return RPM_TEMPLATES.stream();
    }

}
