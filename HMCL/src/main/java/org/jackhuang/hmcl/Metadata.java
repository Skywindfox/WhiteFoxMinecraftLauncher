/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2021  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl;

import org.jackhuang.hmcl.util.StringUtils;
import org.jackhuang.hmcl.util.io.JarUtils;
import org.jackhuang.hmcl.util.platform.Architecture;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

/**
 * Stores metadata about this application.
 */
public final class Metadata {
    private Metadata() {
    }

    /// Short product name, used in window titles, logs and the `${launcher_name}` game placeholder.
    public static final String NAME = "WhiteFox";

    /// Full product name shown on the about page and by the Windows shell.
    public static final String FULL_NAME = "WhiteFox Minecraft Launcher";

    public static final String VERSION = System.getProperty("hmcl.version.override", JarUtils.getAttribute("hmcl.version", "@develop@"));

    /// Explicit Application User Model ID used for Windows taskbar grouping and pinning.
    ///
    /// This fork uses its own ID so that a pinned shortcut is not merged with the upstream launcher,
    /// whose window would otherwise be treated as the same application.
    public static final String WINDOWS_APP_USER_MODEL_ID = "org.whitefox.launcher";

    public static final String TITLE = NAME + " " + VERSION;
    public static final String FULL_TITLE = FULL_NAME + " v" + VERSION;

    public static final int MINIMUM_REQUIRED_JAVA_VERSION = 17;
    public static final int MINIMUM_SUPPORTED_JAVA_VERSION = 17;
    public static final int RECOMMENDED_JAVA_VERSION = 21;

    /// Base URL of the launcher website, used by the about page, error messages and download links.
    ///
    /// Forks can point this elsewhere with the `hmcl.fork.website` build attribute
    /// (see `config/fork.properties`); the upstream site is only a fallback.
    public static final String PUBLISH_URL = JarUtils.getAttribute("hmcl.fork.website", "https://github.com/Skywindfox/WhiteFoxMinecraftLauncher");
    public static final String DOWNLOAD_URL = PUBLISH_URL + "/download";
    public static final String HMCL_UPDATE_URL = System.getProperty("hmcl.update_source.override",
            JarUtils.getAttribute("hmcl.fork.update_url", PUBLISH_URL + "/api/update_link"));
    public static final String MANUAL_UPDATE_URL = JarUtils.getAttribute("hmcl.fork.manual_update_url", "https://github.com/Skywindfox/WhiteFoxMinecraftLauncher/releases");

    /// Whether this build may check for, download and install launcher updates published by the
    /// official HMCL release pipeline.
    ///
    /// This is an unofficial fork, so it is disabled unless the `hmcl.self_update.enabled` system
    /// property is set to `true`. Installing an official build would silently replace this launcher
    /// and discard fork-specific behavior, so a fork must never let that happen by accident.
    public static final boolean SELF_UPDATE_ENABLED = Boolean.getBoolean("hmcl.self_update.enabled");

    /// Documentation site. Derived URLs assume the site provides the same paths as upstream, so a
    /// fork that sets `hmcl.fork.docs` must also serve `/changelog/`, `/help.html` and `/eula/hmcl.html`.
    public static final String DOCS_URL = JarUtils.getAttribute("hmcl.fork.docs", "https://docs.hmcl.net");
    public static final String CONTACT_URL = DOCS_URL + "/help.html";
    public static final String CHANGELOG_URL = DOCS_URL + "/changelog/";
    public static final String EULA_URL = DOCS_URL + "/eula/hmcl.html";
    public static final String GROUPS_URL = "https://www.bilibili.com/opus/905435541874409529";

    public static final String BUILD_CHANNEL = JarUtils.getAttribute("hmcl.version.type", "nightly");
    public static final String GITHUB_SHA = JarUtils.getAttribute("hmcl.version.hash", null);

    public static final Path CURRENT_DIRECTORY = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    public static final Path MINECRAFT_DIRECTORY = OperatingSystem.getWorkingDirectory("minecraft");
    public static final Path HMCL_USER_HOME;
    public static final Path HMCL_LOCAL_HOME;
    public static final Path DEPENDENCIES_DIRECTORY;

    static {
        String hmclHome = System.getProperty("hmcl.home", System.getenv("HMCL_USER_HOME"));
        if (StringUtils.isBlank(hmclHome)) {
            if (OperatingSystem.CURRENT_OS.isLinuxOrBSD()) {
                String xdgData = System.getenv("XDG_DATA_HOME");
                if (StringUtils.isNotBlank(xdgData)) {
                    HMCL_USER_HOME = Path.of(xdgData, "hmcl").toAbsolutePath().normalize();
                } else {
                    HMCL_USER_HOME = Path.of(System.getProperty("user.home"), ".local", "share", "hmcl").toAbsolutePath().normalize();
                }
            } else {
                HMCL_USER_HOME = OperatingSystem.getWorkingDirectory("hmcl");
            }
        } else {
            HMCL_USER_HOME = Path.of(hmclHome).toAbsolutePath().normalize();
        }

        String hmclCurrentDir = System.getProperty("hmcl.dir", System.getenv("HMCL_LOCAL_HOME"));
        HMCL_LOCAL_HOME = StringUtils.isNotBlank(hmclCurrentDir)
                ? Path.of(hmclCurrentDir).toAbsolutePath().normalize()
                : CURRENT_DIRECTORY.resolve(".hmcl");

        String hmclDependencies = System.getProperty("hmcl.dependencies.dir", System.getenv("HMCL_DEPENDENCIES_DIR"));
        DEPENDENCIES_DIRECTORY = StringUtils.isNotBlank(hmclDependencies)
                ? Path.of(hmclDependencies).toAbsolutePath().normalize()
                : HMCL_LOCAL_HOME.resolve("dependencies");
    }

    public static boolean isStable() {
        return "stable".equals(BUILD_CHANNEL);
    }

    public static boolean isDev() {
        return "dev".equals(BUILD_CHANNEL);
    }

    public static boolean isNightly() {
        return !isStable() && !isDev();
    }

    public static @Nullable String getSuggestedJavaDownloadLink() {
        if (OperatingSystem.CURRENT_OS == OperatingSystem.LINUX && Architecture.SYSTEM_ARCH == Architecture.LOONGARCH64_OW)
            return "https://www.loongnix.cn/zh/api/java/downloads-jdk21/index.html";
        else {
            EnumSet<Architecture> supportedArchitectures;
            if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS)
                supportedArchitectures = EnumSet.of(Architecture.X86_64, Architecture.X86, Architecture.ARM64);
            else if (OperatingSystem.CURRENT_OS == OperatingSystem.LINUX)
                supportedArchitectures = EnumSet.of(
                        Architecture.X86_64, Architecture.X86,
                        Architecture.ARM64, Architecture.ARM32,
                        Architecture.RISCV64, Architecture.LOONGARCH64
                );
            else if (OperatingSystem.CURRENT_OS == OperatingSystem.MACOS)
                supportedArchitectures = EnumSet.of(Architecture.X86_64, Architecture.ARM64);
            else
                supportedArchitectures = EnumSet.noneOf(Architecture.class);
            if (supportedArchitectures.contains(Architecture.SYSTEM_ARCH))
                return String.format("https://docs.hmcl.net/downloads/%s/%s.html",
                        OperatingSystem.CURRENT_OS.getCheckedName(),
                        Architecture.SYSTEM_ARCH.getCheckedName()
                );
            else
                return null;
        }
    }

    /// Directory under [HMCL_LOCAL_HOME] that holds a launcher-bundled modpack for automatic install.
    public static final String BUNDLED_MODPACK_DIRECTORY_NAME = "modpack";

    /// Returns the directory for a launcher-bundled modpack (`[HMCL_LOCAL_HOME]/modpack`).
    public static Path getBundledModpackDirectory() {
        return HMCL_LOCAL_HOME.resolve(BUNDLED_MODPACK_DIRECTORY_NAME);
    }

    /// Returns the bundled modpack package under [getBundledModpackDirectory], if present.
    ///
    /// Prefers `modpack.zip` over `modpack.mrpack` when both exist. Presence of the package is the
    /// signal to offer automatic install; the file is removed after a successful install.
    ///
    /// @return the modpack path, or `null` when no package is present
    public static @Nullable Path findBundledModpackFile() {
        Path directory = getBundledModpackDirectory();
        Path zipModpack = directory.resolve("modpack.zip");
        if (Files.isRegularFile(zipModpack)) {
            return zipModpack;
        }
        Path mrpackModpack = directory.resolve("modpack.mrpack");
        if (Files.isRegularFile(mrpackModpack)) {
            return mrpackModpack;
        }
        return null;
    }

}
