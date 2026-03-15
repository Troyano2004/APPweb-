package com.erwin.backend.service;

import com.erwin.backend.dtos.RevisionIARequest;
import com.erwin.backend.entities.DocumentoTitulacion;
import com.erwin.backend.repository.DocumentoTitulacionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RevisionIAService {

    @Autowired
    private DocumentoTitulacionRepository documentoRepository;

    private final String groqApiUrl = "https://api.groq.com/openai/v1/chat/completions";
    private final String groqApiKey = ""; // <-- pega tu key aquí

    public DocumentoTitulacion evaluarTituloYObjetivos(Integer documentoId, RevisionIARequest request) {
        DocumentoTitulacion doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        String titulo = doc.getTitulo() != null ? doc.getTitulo() : "";
        String objetivoGeneral = doc.getObjetivoGeneral() != null ? doc.getObjetivoGeneral() : "";
        String objetivosEspecificos = doc.getObjetivosEspecificos() != null ? doc.getObjetivosEspecificos() : "";

        String respuestaIA = llamarApiDeGroq(titulo, objetivoGeneral, objetivosEspecificos, request);

        doc.setEstadoRevisionIa("EVALUADO");
        doc.setFeedbackIa(respuestaIA);
        doc.setFechaRevisionIa(LocalDateTime.now());

        return documentoRepository.save(doc);
    }

    private String llamarApiDeGroq(String titulo,
                                   String objGeneral,
                                   String objEspecificos,
                                   RevisionIARequest request) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + groqApiKey); // <-- así funciona Groq

        String modo = request != null && request.getModo() != null ? request.getModo().trim() : "";
        String promptPersonalizado = request != null && request.getPromptPersonalizado() != null
                ? request.getPromptPersonalizado().trim()
                : "";

        String promptBase = construirPromptBasePorModo(modo);
        String promptFinal = construirPromptFinal(promptBase, promptPersonalizado, titulo, objGeneral, objEspecificos);

        // Formato OpenAI: messages con role system + user
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", promptBase);

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", promptFinal);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama-3.3-70b-versatile"); // modelo gratuito de Groq
        requestBody.put("messages", List.of(systemMessage, userMessage));
        requestBody.put("temperature", 0.3);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            String responseBody = restTemplate.postForObject(groqApiUrl, requestEntity, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseBody);

            // Formato OpenAI: choices[0].message.content
            JsonNode textNode = rootNode.path("choices").get(0)
                    .path("message").path("content");

            return textNode.asText().replaceAll("```json", "").replaceAll("```", "").trim();

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"estado\": \"ERROR\", \"feedback\": \"Error al comunicarse con Groq: " + e.getMessage() + "\"}";
        }
    }

    private String construirPromptBasePorModo(String modo) {
        switch (modo) {
            case "estilo-academico":
                return "Eres un corrector académico experto. Evalúa redacción, gramática, cohesión y claridad. " +
                        "Propón mejoras concretas y ejemplos de reformulación.";
            case "metodologia-rigor":
                return "Eres un metodólogo de investigación. Evalúa rigor, coherencia metodológica, variables, " +
                        "validez y viabilidad. Entrega hallazgos críticos y acciones de mejora.";
            case "innovacion-impacto":
                return "Eres un evaluador de innovación. Determina nivel de originalidad, impacto potencial y " +
                        "factibilidad de implementación, proponiendo mejoras creativas.";
            case "evaluacion-integral":
            default:
                return "Eres un evaluador académico experto. Revisa alineación entre título, objetivo general y " +
                        "objetivos específicos, destacando fortalezas y oportunidades de mejora.";
        }
    }

    private String construirPromptFinal(String promptBase,
                                        String promptPersonalizado,
                                        String titulo,
                                        String objGeneral,
                                        String objEspecificos) {
        String instruccionUsuario = !promptPersonalizado.isBlank()
                ? "Instrucción adicional del docente: " + promptPersonalizado + "\n"
                : "";

        return promptBase + "\n" +
                instruccionUsuario +
                "Título: " + titulo + "\n" +
                "Objetivo General: " + objGeneral + "\n" +
                "Objetivos Específicos: " + objEspecificos + "\n\n" +
                "Responde ÚNICAMENTE en JSON válido con este formato: " +
                "{\"estado\":\"ALINEADO|REQUIERE_MODIFICACION\",\"feedback\":\"texto\",\"sugerencias\":[\"...\"]}";
    }
}
