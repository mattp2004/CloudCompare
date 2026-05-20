package org.cloudcompare.backend.ai;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;

@Service
public class SbertEngine {
    OrtEnvironment environment;
    OrtSession session;
    HuggingFaceTokenizer tokenizer;

    private final String modelName = "models/model.onnx";
    private final String tokenizerName = "models/tokenizer.json";


    @PostConstruct
    public void init() throws OrtException {

        environment = OrtEnvironment.getEnvironment();
        Path path;
        try(var modelStream = getClass().getClassLoader().getResourceAsStream(modelName)) {
            if(modelStream == null) throw new RuntimeException("Model not found.");
            File tempModel = File.createTempFile("sbert_model", ".onnx");
            tempModel.deleteOnExit();
            Files.copy(modelStream,tempModel.toPath(), StandardCopyOption.REPLACE_EXISTING);
            session = environment.createSession(tempModel.getAbsolutePath(), new OrtSession.SessionOptions());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (var tokenizerStream= getClass().getClassLoader().getResourceAsStream(tokenizerName)){
            if(tokenizerStream == null) throw new RuntimeException("Model not found.");
            File tempTokenizer = File.createTempFile("tokenizer", ".json");
            tempTokenizer.deleteOnExit();
            Files.copy(tokenizerStream, tempTokenizer.toPath(), StandardCopyOption.REPLACE_EXISTING);
            tokenizer = HuggingFaceTokenizer.newInstance(tempTokenizer.toPath());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public double executeSbert(String ServiceA, String ServiceB) throws OrtException {
        float[] embeddingA = generateEmbedding(ServiceA);
        float[] embeddingB = generateEmbedding(ServiceB);

        double rawSimilarityScore = calculateCosineSimilarity(embeddingA, embeddingB);
        double normalised = (rawSimilarityScore + 1.0) / 2.0;
        return Math.min(1.0, normalised);
    }

    private float[] generateEmbedding(String text) throws OrtException {
        Encoding encodedText = tokenizer.encode(text);

        long[][] ids =  new long[][]{encodedText.getIds()};
        long[][] attentionMask =  new long[][]{encodedText.getAttentionMask()};
        long[][] typeIds =  new long[][]{encodedText.getTypeIds()};
        OnnxTensor inputIdsTensor = OnnxTensor.createTensor(environment, ids);
        OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(environment, attentionMask);
        OnnxTensor typeIdTensor = OnnxTensor.createTensor(environment, typeIds);

        var inputs = Map.of("input_ids",inputIdsTensor,"attention_mask", attentionMaskTensor,"token_type_ids", typeIdTensor);

        try(var results = session.run(inputs)){
            float[][][] embeddings = (float[][][]) results.get(0).getValue();
            return meanPool(embeddings, encodedText.getAttentionMask());
        }
    }

    private float[] meanPool(float[][][] embeddings, long[] attentionMask) {
        float[] pooled = new float[embeddings[0][0].length];
        int count = 0;
        for(int i = 0; i < embeddings[0].length; i++){
            if(attentionMask[i] == 1){
                for(int j = 0 ; j < pooled.length; j++){
                    pooled[j] += embeddings[0][i][j];
                }
                count+=1;
            }
        }
        if(count>0){
            for(int j = 0 ; j < pooled.length; j++){
                pooled[j] = pooled[j] /count;
            }
        }
        return pooled;
    }

    private double calculateCosineSimilarity(float[] embeddingA, float[] embeddingB) {
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        for(int i = 0; i < embeddingA.length; i++){
            dotProduct += embeddingA[i] * embeddingB[i];
            normA += embeddingA[i] * embeddingA[i];
            normB += embeddingB[i] * embeddingB[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
