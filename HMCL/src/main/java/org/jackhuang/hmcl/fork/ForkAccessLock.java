/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2026 huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.jackhuang.hmcl.fork;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import org.jackhuang.hmcl.Metadata;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.jackhuang.hmcl.util.i18n.I18n.i18n;
import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// A one-time access gate for this fork.
///
/// The first launch asks for an access code; after a correct code the launch is recorded in
/// [`#MARKER_NAME`] and later launches start normally. Wrong codes and cancellation keep the launcher
/// closed, so the program cannot be used without the code.
///
/// This is a soft gate against casual redistribution, **not** a security boundary: the code lives in
/// this source file on purpose so the maintainer can hand it out and readers can find it, and anyone
/// willing to rebuild from source can remove the check entirely.
@NotNullByDefault
public final class ForkAccessLock {

    /// The access code that unlocks this build.
    ///
    /// Change this constant to rotate the code: existing `access.lock` markers store a hash of the old
    /// code and therefore stop matching, which forces every installation to enter the new code.
    private static final String ACCESS_CODE = "WF-7Q2M-9K3P";

    /// Marker file recording a successful unlock, relative to {@link Metadata#HMCL_LOCAL_HOME}.
    private static final String MARKER_NAME = "access.lock";

    /// Returns whether this installation was already unlocked with the current access code.
    ///
    /// The marker stores a hash of the code rather than the code itself, so rotating [`#ACCESS_CODE`]
    /// invalidates every existing marker instead of leaving stale ones valid.
    public static boolean isUnlocked() {
        Path marker = markerFile();
        if (!Files.isRegularFile(marker))
            return false;

        try {
            String recorded = Files.readString(marker, StandardCharsets.UTF_8).trim();
            return recorded.equals(hashOf(ACCESS_CODE));
        } catch (IOException e) {
            LOG.warning("Failed to read the access marker " + marker, e);
            return false;
        }
    }

    /// Checks the access code, and on success makes later launches skip the gate.
    ///
    /// Returns `false` when the code is missing, wrong or the write of the marker fails; callers must
    /// treat `false` as "do not start the launcher".
    public static boolean tryUnlock(@Nullable String candidate) {
        if (candidate == null || !hashOf(candidate.trim()).equals(hashOf(ACCESS_CODE)))
            return false;

        Path marker = markerFile();
        try {
            // The config directory may not exist yet on a brand new installation.
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, hashOf(ACCESS_CODE), StandardCharsets.UTF_8);
            LOG.info("Launcher unlocked, marker written to " + marker);
            return true;
        } catch (IOException e) {
            LOG.warning("Failed to write the access marker " + marker, e);
            return false;
        }
    }

    /// Blocks until the correct access code is entered, or returns `false` if the user gives up.
    ///
    /// Shows a modal dialog; must be called on the JavaFX thread before the main window is shown.
    public static boolean ensureUnlocked() {
        if (isUnlocked())
            return true;

        while (true) {
            TextField input = new TextField();
            input.setPromptText(i18n("fork.lock.field"));

            ButtonType unlockButton = new ButtonType(i18n("button.ok"), ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType(i18n("button.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

            Alert alert = new Alert(Alert.AlertType.NONE);
            alert.setTitle(i18n("fork.lock.title"));
            alert.setHeaderText(i18n("fork.lock.header"));
            alert.getDialogPane().setContent(input);
            alert.getButtonTypes().setAll(unlockButton, cancelButton);

            ButtonType pressed = alert.showAndWait().orElse(cancelButton);
            if (pressed != unlockButton)
                return false;

            if (tryUnlock(input.getText()))
                return true;

            LOG.info("Rejected an incorrect access code");
        }
    }

    /// Returns the marker file used to remember a successful unlock.
    private static Path markerFile() {
        return Metadata.HMCL_LOCAL_HOME.resolve(MARKER_NAME);
    }

    /// Hashes a code with SHA-256 and returns lowercase hexadecimal.
    private static String hashOf(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandatory for every Java platform, so this branch is unreachable in practice.
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private ForkAccessLock() {
    }
}
