package com.memeboo2.haemi.m0.application.command;

import java.util.UUID;

public record TagPhotoPersonCommand(UUID personId, double confidence, boolean confirmed) {
}
