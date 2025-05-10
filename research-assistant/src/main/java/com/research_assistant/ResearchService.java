package com.research_assistant;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.research_assistant.dto.GeminiResponse;
import com.research_assistant.dto.ResearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class ResearchService {
    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    @Value("${gemini.api.key}")
    private  String geminiApiKey;


    private final WebClient webClient ;
    private final ObjectMapper objectMapper;

    public ResearchService(WebClient.Builder webclientBuider,ObjectMapper objectMapper){
        this.webClient = webclientBuider.build();
        this.objectMapper = objectMapper;
    }

    public  String processContent(ResearchRequest request) {
        //Build the prompt
        String prompt = buildPrompt(request);
        Map<String, Object[]> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts",new Object[]{
                                Map.of("text",prompt)
                        })
                }
        );
        //Query the Ai model API
        String response = webClient.post()
                .uri(geminiApiUrl+geminiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        //Parse the Response
        return extractTextFromResponse(response);
        //Return response
    }

    private String extractTextFromResponse(String response) {
        try {
            GeminiResponse geminiResponse = objectMapper.readValue(response, GeminiResponse.class);
            if(geminiResponse.getCandidates()!=null && !geminiResponse.getCandidates().isEmpty()){
                GeminiResponse.Candidate firstCandidate = geminiResponse.getCandidates().get(0);
                if(firstCandidate.getContent()!= null &&
                                        firstCandidate.getContent().getParts()!= null &&
                                        !firstCandidate.getContent().getParts().isEmpty()){
                    return firstCandidate.getContent().getParts().get(0).getText();
                }
            }
            return "No content found";
        }

        catch (Exception e){
            return "Error Parsing :" +e.getMessage();
        }
    }

    private  String buildPrompt(ResearchRequest request){
         StringBuilder prompt = new StringBuilder();
         switch (request.getOperation()){
             case "summarize":
                 prompt.append("Summarize the following text in a few concise lines, focusing only on the key points and main message:");
                 break;
             case "suggest":
                 prompt.append("Read the following text and provide a few concise suggestions or improvements, focusing only on the most important points:");
                 break;
             default:
                 throw new IllegalArgumentException("Unknown Operation: "+ request.getOperation());
         }
         prompt.append(request.getContent());
         return prompt.toString();
    }
}
