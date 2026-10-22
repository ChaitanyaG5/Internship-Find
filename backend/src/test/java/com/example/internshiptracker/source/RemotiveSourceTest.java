package com.example.internshiptracker.source;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RemotiveSourceTest {

    @Test
    void parsesRemotiveLocalDateTimeAsUtc() {
        assertThat(RemotiveSource.parsePublicationDate("2026-10-17T16:04:29"))
                .isEqualTo(Instant.parse("2026-10-17T16:04:29Z"));
    }

    @Test
    void preservesExplicitTimestampOffset() {
        assertThat(RemotiveSource.parsePublicationDate("2026-10-17T18:04:29+02:00"))
                .isEqualTo(Instant.parse("2026-10-17T16:04:29Z"));
    }
}
