package com.memeboo2.haemi.common.support;

import com.memeboo2.haemi.common.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainIdsTest {

    @Test
    void 정상_UUID는_그대로_파싱된다() {
        UUID expected = UUID.randomUUID();

        assertThat(DomainIds.parseUuid(expected.toString(), "앨범 ID")).isEqualTo(expected);
    }

    @Test
    void 앞뒤_공백은_잘라낸다() {
        UUID expected = UUID.randomUUID();

        assertThat(DomainIds.parseUuid("  " + expected + "  ", "앨범 ID")).isEqualTo(expected);
    }

    @Test
    void 형식이_어긋나면_도메인_검증_예외로_바뀐다() {
        assertThatThrownBy(() -> DomainIds.parseUuid("not-a-uuid", "앨범 ID"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("앨범 ID는 UUID 형식이어야 해요.");
    }

    /** UUID.fromString의 "Invalid UUID string: ..." 메시지가 응답으로 새어 나가면 안 된다. */
    @Test
    void 내부_예외_메시지는_원인으로만_남는다() {
        assertThatThrownBy(() -> DomainIds.parseUuid("not-a-uuid", "앨범 ID"))
                .hasMessageNotContaining("Invalid UUID string")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 비어_있으면_필수값_안내로_막는다() {
        assertThatThrownBy(() -> DomainIds.parseUuid("   ", "앨범 ID"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("앨범 ID는 필수예요.");

        assertThatThrownBy(() -> DomainIds.parseUuid(null, "앨범 ID"))
                .isInstanceOf(DomainValidationException.class);
    }
}
