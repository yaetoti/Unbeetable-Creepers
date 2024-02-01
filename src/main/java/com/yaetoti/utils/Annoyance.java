package com.yaetoti.utils;

import com.google.common.collect.Range;

public enum Annoyance {
    ANNOY(Range.closedOpen(0.0, 0.4)),
    SCARE(Range.closedOpen(0.4, 0.65)),
    KILL(Range.closedOpen(0.65, 0.9)),
    REVENGE(Range.closed(0.9, 1.0));

    private final Range<Double> range;

    Annoyance(Range<Double> range) {
        this.range = range;
    }

    public Range<Double> getRange() {
        return range;
    }

    public static Annoyance of(double annoyance) {
        for (var entry : Annoyance.values()) {
            if (entry.getRange().contains(annoyance)) {
                return entry;
            }
        }

        throw new IllegalArgumentException("Invalid annoyance level: " + annoyance);
    }
}
