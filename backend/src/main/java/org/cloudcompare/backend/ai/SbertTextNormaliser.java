package org.cloudcompare.backend.ai;

import org.cloudcompare.backend.catalog.ServiceOffer;

import java.util.Map;

public class SbertTextNormaliser {
    private SbertTextNormaliser(){
        //
    }
    public static String buildSbertText(ServiceOffer offer){
        StringBuilder builder = new StringBuilder();

        builder.append(clean(offer.getName()));
        builder.append(clean(offer.getDescription()));
        builder.append(clean(offer.getProvider()));
        builder.append(clean(offer.getOfferType().name()));

        Map<String, Object> specs = offer.getSpecsJson();
        if(specs!= null){
            cleanAddSpec(builder,"instance_type", specs);
            cleanAddSpec(builder,"operating_system", specs);
            cleanAddSpec(builder,"database_engine", specs);
            cleanAddSpec(builder,"product_family", specs);

            cleanAddSpec(builder,"raw_type", specs);
            cleanAddSpec(builder,"service_family", specs);
            cleanAddSpec(builder,"reservationTerm", specs);

            cleanAddSpec(builder,"resource_family", specs);
            cleanAddSpec(builder,"resource_group", specs);

            cleanAddSpec(builder,"usage_type", specs);
            cleanAddSpec(builder,"raw_unit", specs);
        }


        return builder.toString();
    }

    private static void cleanAddSpec(StringBuilder builder, String specName, Map<String,Object> specs){
        Object specValue = specs.get(specName);
        if(specValue == null || specValue.toString().isBlank()){
            return;
        }
        builder.append(specValue.toString());

    }

    private static String clean(String s) {
        if(s!=null){
            return s;
        }
        return "";
    }
}
