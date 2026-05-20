package org.cloudcompare.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudcompare.backend.util.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AiComparisonService {

    private final RestClient http = RestClient.create();
    private final ObjectMapper objMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String OPENAI_KEY;

    //prompt for optimal output.
    public String compareOffers(String jsonA, String jsonB) {
        try {
            String prompt = """
            You are a cloud solution architecture.
            
            Please compare the following two cloud sercices and produce a structured and understandable compatibility report. Please do not exceed 150 words. 

            Be professional, insightful, avoid aggressive or harsh language, you are assisting in a cloud migration analysis.             
            Use formatting and emojis to improve readability . 
                        
            Output the report EXACTLY in the FOLLOWING structure:
            
            🔎 Service Overview
            • Service A: [1 sentence summary]
            • Service B: [1 sentence summary]
            
            🧩 Compatibility Score
            • Score: [0–100]% 
            • Verdict: [Highly Compatible>80% / Partially Compatible>60% / Limited Compatibility>40% / Not Directly Compatible>30%>
            
            ⚙️ Technical Comparison
            • Service Type:
            • Engine & API Compatibility:
            • Sizing & Units:
            • Performance (if relevant):
            
            🚚 Migration Difficulty
            • Level: [Low / Moderate / High]
            • Reasoning: [1–2 sentences explanation]
            
            Guidelines:
            - Focus only on technical compatibility.
            - Be realistic but not harsh or super negative.
            - Consider unit difference (Gib vs GB)
            - If services are fundamentally different types, explain clearly.
            - Keep tone analytical.
            - Do not exceed 150-200 words.
            
            SERVICE A:
            """ + jsonA+ """
            
            SERVICE B:
            """ + jsonB+".";


            String body = objMapper.writeValueAsString(objMapper.createObjectNode().put("model", "gpt-5-mini").put("input", prompt));

            String rawReply =
                    http.post().uri("https://api.openai.com/v1/responses").header("Authorization", "Bearer " + OPENAI_KEY).header("Content-Type", "application/json").body(body).retrieve().body(String.class);

            System.out.println("RAW REPLY FROM LLM :\n" + rawReply);

            JsonNode root = objMapper.readTree(rawReply);


            for(int i = 0; i < root.path("output").size(); i++){
                JsonNode item = root.path("output").get(i);
                if(item.path("type").asText().equals("message")){
                    for(JsonNode c : item.path("content")){
                        if(c.path("type").asText().equals("output_text")){
                            String out = c.path("text").asText();
                            if(!out.isEmpty()){
                                return out;
                            }
                        }
                    }
                }

            }
            return "No response - AI";

        } catch (Exception e) {
            Logger.LogError(e.getMessage());
        }
        return "No response - AI";
    }

}
