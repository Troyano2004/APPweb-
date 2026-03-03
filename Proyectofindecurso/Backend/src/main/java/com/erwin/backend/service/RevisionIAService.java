package com.erwin.backend.service; // Asegúrate de que este sea tu paquete correcto

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

    // URL usando "latest" para que nunca caduque el modelo
    private final String geminiApiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    // TODO: Pega tu clave real aquí
    private final String geminiApiKey = "";

    /**
     * Este es el método que falta y que el Controlador está intentando llamar.
     * Se encarga de buscar el documento, mandarlo a la IA y guardar el resultado.
     */
    public DocumentoTitulacion evaluarTituloYObjetivos(Integer documentoId) {
        // 1. Obtener el documento de la base de datos
        DocumentoTitulacion doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        String titulo = doc.getTitulo() != null ? doc.getTitulo() : "";
        String objetivoGeneral = doc.getObjetivoGeneral() != null ? doc.getObjetivoGeneral() : "";
        String objetivosEspecificos = doc.getObjetivosEspecificos() != null ? doc.getObjetivosEspecificos() : "";

        // 2. Llamar a la API de Google Gemini
        String respuestaIA = llamarApiDeGemini(titulo, objetivoGeneral, objetivosEspecificos);

        // 3. Guardar el resultado en la base de datos
        doc.setEstadoRevisionIa("EVALUADO");
        doc.setFeedbackIa(respuestaIA);
        doc.setFechaRevisionIa(LocalDateTime.now());

        return documentoRepository.save(doc);
    }

    /**
     * Este método hace la petición HTTP POST a Google imitando el comando curl.
     */
    private String llamarApiDeGemini(String titulo, String objGeneral, String objEspecificos) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        // Configuramos las cabeceras (pasando la API Key de forma segura)
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-goog-api-key", geminiApiKey);

        // Creamos la instrucción detallada para la IA
        String prompt = String.format("Eres un evaluador académico experto. Revisa si los siguientes objetivos " +
                        "están correctamente alineados y se adaptan al título de la tesis.\n" +
                        "Título: %s\nObjetivo General: %s\nObjetivos Específicos: %s\n\n" +
                        "Responde únicamente con un formato JSON válido con dos propiedades: " +
                        "'estado' (ALINEADO o REQUIERE_MODIFICACION) y 'feedback' (tu explicación detallada).",
                titulo, objGeneral, objEspecificos);

        // Construimos la estructura JSON que pide Gemini
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> parts = new HashMap<>();
        parts.put("text", prompt);

        Map<String, Object> contents = new HashMap<>();
        contents.put("parts", List.of(parts));

        requestBody.put("contents", List.of(contents));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            // Ejecutamos la petición POST
            String responseBody = restTemplate.postForObject(geminiApiUrl, request, String.class);

            // Parseamos la respuesta para extraer solo el texto generado
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseBody);

            JsonNode textNode = rootNode.path("candidates").get(0)
                    .path("content").path("parts").get(0).path("text");

            // Limpiamos la respuesta en caso de que la IA le agregue formato markdown (```json)
            return textNode.asText().replaceAll("```json", "").replaceAll("```", "").trim();

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"estado\": \"ERROR\", \"feedback\": \"Error al comunicarse con Gemini: " + e.getMessage() + "\"}";
        }
    }
}