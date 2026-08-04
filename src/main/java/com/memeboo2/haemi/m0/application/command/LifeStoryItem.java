package com.memeboo2.haemi.m0.application.command;

import com.memeboo2.haemi.m0.domain.model.LifeStoryCategory;
import com.memeboo2.haemi.m0.domain.model.LifeStorySource;

public record LifeStoryItem(LifeStoryCategory category, String value, Integer weight, LifeStorySource source) {
}
