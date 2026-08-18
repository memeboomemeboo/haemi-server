package com.memeboo2.haemi.m1.domain.model.album;

/**
 * 앨범 사진 등록 시 선택 가능한 시기 옵션.
 * 클라이언트는 디자인 하드코딩이 아닌 API 반환값을 따른다.
 */
public enum AlbumTimePeriod {

    BEFORE_1950("1950년대 이전"),
    DECADE_1950("1950년대"),
    DECADE_1960("1960년대"),
    DECADE_1970("1970년대"),
    DECADE_1980("1980년대"),
    DECADE_1990("1990년대"),
    DECADE_2000("2000년대"),
    DECADE_2010("2010년대"),
    DECADE_2020("2020년대"),
    UNKNOWN("날짜 미상");

    private final String label;

    AlbumTimePeriod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
