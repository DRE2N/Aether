package de.erethon.aether.tools;

import org.jetbrains.annotations.Nullable;

public record ErrorEntry(String locationIdentifier, String friendlyMessage, String errorMessage, @Nullable StackTraceElement[] stackFrames) {
}
