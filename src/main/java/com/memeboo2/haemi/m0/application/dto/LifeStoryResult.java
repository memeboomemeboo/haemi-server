package com.memeboo2.haemi.m0.application.dto;

import com.memeboo2.haemi.m0.domain.model.LifeStory;
import com.memeboo2.haemi.m0.domain.model.LifeStoryCategory;
import com.memeboo2.haemi.m0.domain.model.LifeStorySource;

public record LifeStoryResult(LifeStoryCategory category, String value, int weight, LifeStorySource source) {
    public static LifeStoryResult from(LifeStory lifeStory) {
        return new LifeStoryResult(lifeStory.getCategory(), lifeStory.getValue(), lifeStory.getWeight(),
                lifeStory.getSource());
    }
}
