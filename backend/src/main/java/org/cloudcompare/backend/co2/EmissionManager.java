package org.cloudcompare.backend.co2;

import org.cloudcompare.backend.catalog.Compare;
import org.cloudcompare.backend.catalog.ServiceOffer;
import org.cloudcompare.backend.co2.catalog.Emissions;
import org.cloudcompare.backend.co2.catalog.EmissionsMap;
import org.cloudcompare.backend.db.offers.CompareRepo;
import org.cloudcompare.backend.db.offers.ServiceOfferRepo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmissionManager {
    private final CompareRepo compareRepo;
    private final ServiceOfferRepo serviceOfferRepo;

    public EmissionManager(CompareRepo compareRepo, ServiceOfferRepo serviceRepo){
        this.compareRepo = compareRepo;
        this.serviceOfferRepo = serviceRepo;
    }

    public Emissions calculateForOffer(String offerId){
        ServiceOffer offer = compareRepo.getOfferById(offerId);
        if(offer != null){
            return EmissionEngine.calculateEmissions(offer);
        }
        return null;
    }

    public List<Emissions> calculateForComparison(String offerId, int limit){
        Compare comparison = compareRepo.makeComparisonById(offerId, limit);
        if(comparison == null) return List.of();

        List<Emissions> emissions = new ArrayList<>();
        emissions.add(EmissionEngine.calculateEmissions(comparison.getSelectedOffer()));
        for (int i = 0; i < comparison.getComparedOffers().size(); i++){
            emissions.add(EmissionEngine.calculateEmissions(comparison.getComparedOffers().get(i)));
        }
        return emissions;
    }

    public List<EmissionsMap> calculateForMap(String serviceId, String provider, String region, String type){
        List<ServiceOffer> offers = serviceOfferRepo.retrieveByServiceID(serviceId);

        Map<String, Double> co2Totals = new LinkedHashMap<>();
        Map<String, Long> offerCount = new LinkedHashMap<>();
        Map<String, ServiceOffer> serviceRep = new LinkedHashMap<>();

        for(int i = 0;i<offers.size();i++){
            ServiceOffer offer = offers.get(i);
            String key = offer.getRegion() +"|" + offer.getProvider();
            double co2 = EmissionEngine.calculateEmissions(offer).co2KgPerHour;
            co2Totals.put(key, co2Totals.getOrDefault(key,0.0) + co2 );
            offerCount.put(key, offerCount.getOrDefault(key,0L) +1);
            serviceRep.putIfAbsent(key,offer);
        }

        List<String> allKeys = new ArrayList<>(co2Totals.keySet());

        List<EmissionsMap> mapResults = new ArrayList<>();

        for(int i = 0 ; i < allKeys.size(); i++){
            String key = allKeys.get(i);
            ServiceOffer currentOffer = serviceRep.get(key);

            double total = co2Totals.get(key);
            long count = offerCount.get(key);
            double average = total/count;

            String rating = EmissionEngine.rate(average);
            EmissionsMap map = new EmissionsMap(currentOffer.getProvider(), currentOffer.getRegion(), currentOffer.getServiceId(), count, rating,average);
            mapResults.add(map);
        }

        return mapResults;
    }
}
