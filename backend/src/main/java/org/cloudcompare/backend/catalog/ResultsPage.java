package org.cloudcompare.backend.catalog;

import java.util.List;

public class ResultsPage {

    private final int pageNumber;
    private final int pageSize;
    private final long total;
    private final List<ServiceOffer> offers;

    public ResultsPage(int pageNumber, int pageSize, long total, List<ServiceOffer> offers) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.total = total;
        this.offers = offers;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotal() {
        return total;
    }

    public List<ServiceOffer> getOffers() {
        return offers;
    }
}
