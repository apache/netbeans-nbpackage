# Apache NetBeans Packager (nbpackage)

`nbpackage` is a command line tool and library for packaging the NetBeans IDE or
a NetBeans platform application into a native installer or package. It supports
optional bundling of a Java runtime to make a self-contained application. It is
inspired by the JDK's `jpackage` tool, but tailored to the needs of the IDE and
RCP, with a different range of packagers, and some support for cross-platform
package building.

## Usage

Run `nbpackage --help` to see options.

While options for the packagers can be set on the command line, the easiest way
is to use the support for a configuration file in Java Properties format.

Use the `--save-config` option to output all available options, or just the ones
for a specific packager, to a properties file. Using a separate configuration file
for each package type is the best option. The output file includes comments for
each available option.

eg. to build an InnoSetup installer, first create the configuration file -

`nbpackage --type windows-innosetup --save-config inno.properties`

Edit the properties file, with package name, customized icons, path to Java
runtime, etc. As well as the path to the native build tool. Paths can be prefixed
with `${CONFIG}/` to be relative to configuration file location.

Then build the installer from the IDE / RCP zip using -

`nbpackage --config inno.properties --input <PATH_TO_ZIP>`

Use the `--verbose` option to see the output of the native packaging tools.

### Template files

Most packagers use overridable templates, for build files, `.desktop` files, `.plist`
files, etc. To save the existing templates for editing, use -

`nbpackage --type <PACKAGE_TYPE> --save-templates <PATH_TO_FOLDER>`

Edit the files, and add the paths to the relevant properties in the configuration
file (using `${CONFIG}/` relative paths where appropriate).

### Modifying the package image

`nbpackage` will first build an image - a directory with the correct layout and
additional files for passing to the packaging tool. It is possible to use the
`--image-only` and `--input-image` options to perform this in two separate passes
to allow for customization of the process.

## Supported packagers

### `--type linux-appimage`

Create a Linux [AppImage][appimage]. Requires download of the 
[AppImageTool][appimagetool], making it executable, and adding the path to the
configuration file.

### `--type linux-deb`

Create a Linux DEB package. Requires `dpkg`, `dpkg-deb` and `fakeroot` to be
available on the system.

### `--type linux-rpm`

Create a Linux RPM package. Requires `rpm` and `rpmbuild` to be available on the
system.

### `--type macos-pkg`

Create a macOS PKG installer. Requires `swift` to build the launcher, `codesign`
if signing binaries, and `pkgbuild` to build the actual package.

If code signing identities are configured, the package will be signed, as well as
all native binaries including those inside JAR files. The search patterns for
native binaries and JAR files with native binaries can be adapted if necessary.
The built package should then pass the Apple notarization process (submission and
stapling must be done manually).

### `--type windows-innosetup`

Create a Windows [Inno Setup][innosetup] installer. Requires download of the
installer tools, and adding the path to the `iscc` compiler to the packager
configuration. See also [further information on the iscc tool][iscc].

Inno Setup works well with Wine for building installers on other OS. Write a short
shell script that invokes the `iscc` tool via `wine` and use this in the packager
configuration - eg.

```bash
#!/bin/sh
wine C:\\Program\ Files\ \(x86\)\\Inno\ Setup\ 6\\ISCC.exe $1
```

### `--type zip`

Mainly for debugging purposes, although can be used to bundle an RCP application
with local runtime. As the IDE launcher does not yet support relative JDK location,
this is less useful there.

## Building from source

Building `nbpackage` requires Maven and JDK 11+. Building in the IDE or executing
`mvn package` will build the tool. Output can be found in `target/nbpackage-<VERSION>/`,
as well as adjacent source and binary archives.

Run `./nbpackage-<VERSION>/bin/nbpackage --help` to check.

[appimage]: https://appimage.org/
[appimagetool]: https://github.com/AppImage/AppImageKit/releases/
[innosetup]: https://jrsoftware.org/isinfo.php
[iscc]: https://jrsoftware.org/ishelp/index.php?topic=compilercmdline
