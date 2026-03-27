package com.erwin.backend.service;

import com.erwin.backend.dtos.RevisionPropuestaIARequest;
import com.erwin.backend.dtos.RevisionPropuestaIAResponse;
import com.erwin.backend.entities.Carrera;
import com.erwin.backend.entities.EleccionTitulacion;
import com.erwin.backend.entities.Estudiante;
import com.erwin.backend.entities.PropuestaTitulacion;
import com.erwin.backend.repository.EleccionTitulacionRepository;
import com.erwin.backend.repository.PropuestaTitulacionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio IA para evaluacion de propuestas de titulacion.
 *
 * Endpoint: POST /api/revision-ia/propuesta/{idPropuesta}
 * Ruta: src/main/java/com/erwin/backend/service/RevisionPropuestaIAService.java
 */
@Service
public class RevisionPropuestaIAService {

    @Autowired
    private PropuestaTitulacionRepository propuestaRepository;

    @Autowired
    private EleccionTitulacionRepository eleccionRepository;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.api.key}")
    private String groqApiKey;

    // ─── Metodo principal ─────────────────────────────────────────────────

    public RevisionPropuestaIAResponse evaluarPropuesta(Integer idPropuesta,
                                                        RevisionPropuestaIARequest request) {

        PropuestaTitulacion propuesta = propuestaRepository.findById(idPropuesta)
                .orElseThrow(() -> new RuntimeException("Propuesta no encontrada: " + idPropuesta));

        Estudiante estudiante = propuesta.getEstudiante();

        String nombreEstudiante = estudiante.getUsuario().getNombres()
                + " " + estudiante.getUsuario().getApellidos();

        Carrera carrera = propuesta.getCarrera();
        String nombreCarrera = carrera != null ? carrera.getNombre() : "No especificada";
        String nombreFacultad = (carrera != null && carrera.getFacultad() != null)
                ? carrera.getFacultad().getNombre()
                : "No especificada";

        String modalidad = obtenerModalidad(propuesta);

        String feedbackIa = llamarGroq(propuesta, nombreCarrera, nombreFacultad, modalidad, request);

        return new RevisionPropuestaIAResponse(
                idPropuesta,
                propuesta.getTitulo(),
                nombreEstudiante,
                nombreCarrera,
                nombreFacultad,
                modalidad,
                propuesta.getEstado(),
                feedbackIa,
                LocalDateTime.now()
        );
    }

    // ─── Obtener modalidad ────────────────────────────────────────────────

    private String obtenerModalidad(PropuestaTitulacion propuesta) {
        try {
            EleccionTitulacion eleccion = propuesta.getEleccion();
            if (eleccion != null && eleccion.getModalidad() != null) {
                return eleccion.getModalidad().getNombre();
            }

            return eleccionRepository
                    .findByEstudiante_IdEstudiante(propuesta.getEstudiante().getIdEstudiante())
                    .stream()
                    .filter(e -> "ACTIVA".equals(e.getEstado()))
                    .findFirst()
                    .map(e -> e.getModalidad() != null ? e.getModalidad().getNombre() : "No especificada")
                    .orElse("No especificada");

        } catch (Exception e) {
            return "No especificada";
        }
    }

    // ─── Llamada a Groq ───────────────────────────────────────────────────

    private String llamarGroq(PropuestaTitulacion propuesta,
                              String nombreCarrera,
                              String nombreFacultad,
                              String modalidad,
                              RevisionPropuestaIARequest request) {

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + groqApiKey);

        String modo = request != null && request.getModo() != null
                ? request.getModo().trim()
                : "integral";

        String systemPrompt = construirSystemPrompt(modo, nombreCarrera, nombreFacultad, modalidad);
        String userPrompt = construirUserPrompt(propuesta, nombreCarrera, modalidad, request);

        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.3-70b-versatile");
        body.put("messages", List.of(systemMessage, userMessage));
        body.put("temperature", 0.3);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            String raw = restTemplate.postForObject(groqApiUrl, entity, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(raw);

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return buildErrorJson("Groq no devolvio choices validos.");
            }

            String content = choices.get(0)
                    .path("message")
                    .path("content")
                    .asText("");

            return extraerJsonDeRespuesta(content);

        } catch (Exception e) {
            e.printStackTrace();
            return buildErrorJson(e.getMessage());
        }
    }

    /**
     * Limpia la respuesta del modelo y trata de extraer solo el bloque JSON.
     * Casos que cubre:
     * - ```json ... ```
     * - texto antes del JSON
     * - texto despues del JSON
     */
    private String extraerJsonDeRespuesta(String content) {
        if (content == null || content.isBlank()) {
            return buildErrorJson("La IA devolvio una respuesta vacia.");
        }

        String cleaned = content
                .replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1).trim();
        }

        return buildErrorJson("La IA no devolvio un JSON reconocible. Respuesta recibida: " + resumir(cleaned));
    }

    private String resumir(String texto) {
        if (texto == null) return "";
        texto = texto.replace("\"", "'");
        return texto.length() > 180 ? texto.substring(0, 180) + "..." : texto;
    }

    // ─── System Prompts ───────────────────────────────────────────────────

    private String construirSystemPrompt(String modo,
                                         String nombreCarrera,
                                         String nombreFacultad,
                                         String modalidad) {

        String contextoModalidad;
        boolean esComplexivo = modalidad != null
                && modalidad.toUpperCase().contains("COMPLEXIVO");

        if (esComplexivo) {
            contextoModalidad =
                    "El estudiante optara por EXAMEN COMPLEXIVO. En esta modalidad NO se desarrolla " +
                            "un proyecto de software o sistema; el trabajo debe demostrar competencias " +
                            "integradas de la carrera mediante un informe practico aplicado a un caso real. " +
                            "Evalua si la propuesta es adecuada para esta modalidad: debe centrarse en " +
                            "analisis, diagnostico y recomendaciones basadas en conocimientos de la carrera, " +
                            "no en desarrollar una solucion tecnologica nueva. ";
        } else {
            contextoModalidad =
                    "El estudiante optara por TRABAJO DE INTEGRACION CURRICULAR. En esta modalidad " +
                            "se espera el desarrollo de un producto o solucion concreta (sistema, aplicacion, " +
                            "prototipo, investigacion aplicada) que integre los conocimientos de la carrera. " +
                            "Evalua si la propuesta plantea un problema real con una solucion viable y bien delimitada. ";
        }

        String baseContexto =
                "Eres un evaluador academico experto de la " + nombreFacultad + ". " +
                        "Evaluas propuestas de titulacion para la carrera de " + nombreCarrera + " " +
                        "bajo la modalidad: " + modalidad + ". " +
                        contextoModalidad +
                        "Tu criterio SIEMPRE considera si el tema, objetivos y metodologia son " +
                        "pertinentes para el perfil de egreso de " + nombreCarrera + ". " +
                        "Debes responder solamente con JSON valido, sin markdown, sin texto introductorio y sin comentarios.";

        switch (modo) {
            case "coherencia":
                return baseContexto +
                        " Tu analisis se centra en COHERENCIA INTERNA: verifica que el titulo " +
                        "refleje el tema, que los objetivos especificos deriven del general, " +
                        "y que la metodologia sea apropiada para esos objetivos.";

            case "pertinencia":
                return baseContexto +
                        " Tu analisis se centra en PERTINENCIA DISCIPLINAR: evalua si el tema " +
                        "es relevante para " + nombreCarrera + " y adecuado para la modalidad " + modalidad + ". " +
                        "Da ejemplos de temas similares que si o no serian pertinentes.";

            case "viabilidad":
                return baseContexto +
                        " Tu analisis se centra en VIABILIDAD: evalua si el proyecto es realizable " +
                        "en el tiempo de un trabajo de titulacion de pregrado. " +
                        "Indica si el alcance es demasiado ambicioso o demasiado limitado.";

            case "integral":
            default:
                return baseContexto +
                        " Tu analisis es INTEGRAL: coherencia, pertinencia para " + nombreCarrera + ", " +
                        "viabilidad, calidad del planteamiento, solidez de objetivos, metodologia adecuada " +
                        "y realismo de resultados. Da fortalezas, debilidades y sugerencias concretas.";
        }
    }

    // ─── User Prompt ──────────────────────────────────────────────────────

    private String construirUserPrompt(PropuestaTitulacion propuesta,
                                       String nombreCarrera,
                                       String modalidad,
                                       RevisionPropuestaIARequest request) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== CONTEXTO ACADEMICO ===\n");
        sb.append("CARRERA DEL ESTUDIANTE: ").append(nombreCarrera).append("\n");
        sb.append("MODALIDAD DE TITULACION: ").append(modalidad).append("\n\n");

        sb.append("=== PROPUESTA DE TITULACION ===\n");
        sb.append("TITULO: ").append(safe(propuesta.getTitulo())).append("\n\n");
        sb.append("TEMA DE INVESTIGACION: ").append(safe(propuesta.getTemaInvestigacion())).append("\n\n");
        sb.append("PLANTEAMIENTO DEL PROBLEMA:\n").append(safe(propuesta.getPlanteamientoProblema())).append("\n\n");
        sb.append("OBJETIVO GENERAL:\n").append(safe(propuesta.getObjetivosGenerales())).append("\n\n");
        sb.append("OBJETIVOS ESPECIFICOS:\n").append(safe(propuesta.getObjetivosEspecificos())).append("\n\n");
        sb.append("MARCO TEORICO:\n").append(safe(propuesta.getMarcoTeorico())).append("\n\n");
        sb.append("METODOLOGIA:\n").append(safe(propuesta.getMetodologia())).append("\n\n");
        sb.append("RESULTADOS ESPERADOS:\n").append(safe(propuesta.getResultadosEsperados())).append("\n\n");
        sb.append("BIBLIOGRAFIA:\n").append(safe(propuesta.getBibliografia())).append("\n\n");

        if (request != null
                && request.getInstruccionAdicional() != null
                && !request.getInstruccionAdicional().isBlank()) {
            sb.append("INSTRUCCION ADICIONAL: ")
                    .append(request.getInstruccionAdicional())
                    .append("\n\n");
        }

        sb.append("=== INSTRUCCION ===\n");
        sb.append("Evalua esta propuesta de la carrera de ").append(nombreCarrera);
        sb.append(" bajo la modalidad ").append(modalidad).append(".\n");
        sb.append("Verifica si el tema es pertinente para esa carrera y modalidad especifica.\n\n");
        sb.append("Responde UNICAMENTE en JSON valido con este formato exacto, sin markdown y sin texto extra:\n");
        sb.append("{\n");
        sb.append("  \"estado_evaluacion\": \"APROBABLE|REQUIERE_AJUSTES|RECHAZABLE\",\n");
        sb.append("  \"puntaje_estimado\": 0,\n");
        sb.append("  \"pertinencia_carrera\": \"ALTA|MEDIA|BAJA\",\n");
        sb.append("  \"analisis_titulo\": \"analisis del titulo\",\n");
        sb.append("  \"analisis_objetivos\": \"analisis de objetivos\",\n");
        sb.append("  \"analisis_metodologia\": \"analisis de metodologia\",\n");
        sb.append("  \"fortalezas\": [\"fortaleza 1\", \"fortaleza 2\"],\n");
        sb.append("  \"debilidades\": [\"debilidad 1\", \"debilidad 2\"],\n");
        sb.append("  \"sugerencias_mejora\": [\"sugerencia 1\", \"sugerencia 2\"],\n");
        sb.append("  \"mensaje_estudiante\": \"retroalimentacion directa, constructiva y motivadora\"\n");
        sb.append("}");

        return sb.toString();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private String safe(String valor) {
        return valor != null && !valor.isBlank() ? valor : "(no proporcionado)";
    }

    private String buildErrorJson(String errorMsg) {
        String safeError = errorMsg == null
                ? "Error desconocido"
                : errorMsg.replace("\"", "'");

        return "{\"estado_evaluacion\":\"ERROR\","
                + "\"puntaje_estimado\":0,"
                + "\"pertinencia_carrera\":\"ERROR\","
                + "\"analisis_titulo\":\"Error de conexion\","
                + "\"analisis_objetivos\":\"Error de conexion\","
                + "\"analisis_metodologia\":\"Error de conexion\","
                + "\"fortalezas\":[],"
                + "\"debilidades\":[\"Error al comunicarse con la IA: " + safeError + "\"],"
                + "\"sugerencias_mejora\":[\"Intente nuevamente en unos minutos\"],"
                + "\"mensaje_estudiante\":\"Hubo un problema tecnico. Por favor, intente nuevamente.\"}";
    }
}