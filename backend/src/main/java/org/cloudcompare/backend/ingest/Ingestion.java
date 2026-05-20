package org.cloudcompare.backend.ingest;

import org.cloudcompare.backend.catalog.OfferType;
import org.cloudcompare.backend.catalog.Provider;

import java.time.Instant;
import java.util.*;
import java.util.LinkedHashMap;

public class Ingestion {

    public boolean running;
    public Provider provider;

    public Instant startTime;
    public Instant finishTime;

    public long offersStored;
    public long pricesStored;

    public Map<String, Long> errorCounts;

    public Map<OfferType, Long> offerTypeCounts;

    public Ingestion() {
        running = false;
        provider = null;
        startTime = Instant.now();
        finishTime = null;
        offersStored = 0;
        pricesStored = 0;

        errorCounts = new LinkedHashMap<>();

        offerTypeCounts = new EnumMap<>(OfferType.class);
        for (OfferType type : OfferType.values()) {
            offerTypeCounts.put(type, 0L);
        }
    }

    public void incrementOfferType(OfferType type) {
        offerTypeCounts.put(type,
                offerTypeCounts.getOrDefault(type, 0L) + 1);
    }

    public void recordError(String message) {
        if (message == null || message.isBlank()) return;

        errorCounts.put(
                message,
                errorCounts.getOrDefault(message, 0L) + 1
        );
    }
}