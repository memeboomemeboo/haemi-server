package com.memeboo2.haemi.m1.application.command;

import com.memeboo2.haemi.m1.domain.model.reminiscence.ReactionType;

public record RecordReactionCommand(
        String contentId,
        String elderId,
        ReactionType reaction
) {}
