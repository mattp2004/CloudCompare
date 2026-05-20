package org.cloudcompare.backend.ingest;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class IngestionManager {

    public List<Ingestion> ingestions = new ArrayList<>();
    public synchronized void register(Ingestion ingestion) {

        if (ingestion == null) return;

        ingestions.add(ingestion);

    }
    public synchronized List<Ingestion> getAll() {

        return new ArrayList<>(ingestions);
    }

    public Map<String, Object> getStatus() {
        List<Map<String, Object>> currentIngestions = new ArrayList<>();
        List<Map<String, Object>> completedIngestions = new ArrayList<>();

        int runningCount = 0;
        int completedCount = 0;

        Instant lastCompletedAt = null;

        for(int i = 0; i < ingestions.size(); i++){
            Ingestion currentIngestion = ingestions.get(i);
            Map<String,Object> entry = new HashMap<>();
            entry.put("provider", currentIngestion.provider);
            entry.put("running", currentIngestion.running);
            entry.put("startedAt", currentIngestion.startTime);
            entry.put("finishedAt", currentIngestion.finishTime);

            if(currentIngestion.finishTime == null ){
                entry.put("durationSeconds", Duration.between(currentIngestion.startTime,Instant.now()).getSeconds());

            }
            else{
                entry.put("durationSeconds", Duration.between(currentIngestion.startTime,currentIngestion.finishTime));
            }
            entry.put("offersStored",currentIngestion.offersStored);
            entry.put("pricesStored",currentIngestion.pricesStored);
            long t_error_count=0;
            for(Long c :currentIngestion.errorCounts.values()){
                if(c!=null){
                    t_error_count+=c;
                }
            }
            entry.put("errorCount", t_error_count);
            entry.put("errors", currentIngestion.errorCounts);
            entry.put("offerTypeCounts",currentIngestion.offerTypeCounts);
            if(currentIngestion.finishTime!=null){
                if(currentIngestion.finishTime.isAfter(lastCompletedAt)){
                    lastCompletedAt = currentIngestion.finishTime;
                }
            }
            if(currentIngestion.running){
                runningCount+=1;
                currentIngestions.add(entry);
            }
            else{
                completedIngestions.add(entry);
                completedCount+=1;
            }
        }


        Map<String, Object> summary = new HashMap<>();

        summary.put("totalRuns", ingestions.size());
        Map<String, Object> response = new HashMap<>();

        summary.put("running", runningCount);
        summary.put("completed", completedCount);
        summary.put("lastCompletedAt", lastCompletedAt);
        response.put("summary", summary);
        response.put("running", currentIngestions);
        response.put("completed", completedIngestions);

        return response;
    }

}