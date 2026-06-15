package com.memeboo2.haemi.m1.domain.model.album;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoMetadata {

    @Column(name = "shot_at")
    private LocalDateTime shotAt;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "time_period", length = 50)
    private String timePeriod;

    @Column(name = "location_text", length = 100)
    private String locationText;

    @Column(name = "memo", length = 200)
    private String memo;

    private PhotoMetadata(LocalDateTime shotAt, Double latitude, Double longitude,
                          String timePeriod, String locationText, String memo) {
        this.shotAt = shotAt;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timePeriod = timePeriod;
        this.locationText = locationText;
        this.memo = memo;
    }

    public static PhotoMetadata of(LocalDateTime shotAt, Double latitude, Double longitude) {
        return new PhotoMetadata(shotAt, latitude, longitude, null, null, null);
    }

    public PhotoMetadata withMemo(String timePeriod, String locationText, String memo) {
        if (memo != null && memo.length() > 200) {
            throw new MemoLengthExceededException(memo.length());
        }
        return new PhotoMetadata(this.shotAt, this.latitude, this.longitude, timePeriod, locationText, memo);
    }
}
