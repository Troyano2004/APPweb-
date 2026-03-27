
package com.erwin.backend.service;

import com.erwin.backend.dtos.RevisionPropuestaPreviaRequest;
import com.erwin.backend.dtos.RevisionPropuestaIAResponse;
import com.erwin.backend.entities.Carrera;
import com.erwin.backend.entities.EleccionTitulacion;
import com.erwin.backend.entities.Estudiante;
import com.erwin.backend.repository.EleccionTitulacionRepository;
import com.erwin.backend.repository.EstudianteRepository;
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

@Service
public class RevisionPropuestaPreviaService {

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private EleccionTitulacionRepository eleccionRepository;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.api.key}")
    private String groqApiKey;

    // ─── Método principal ─────────────────────────────────────────────────

    public RevisionPropuestaIAResponse evaluarPropuestaPrevia(RevisionPropuestaPreviaRequest request) {

        if (request.getIdEstudiante() == null) {
            throw new RuntimeException("idEstudiante es obligatorio.");
        }

        Estudiante estudiante = estudianteRepository.findById(request.getIdEstudiante())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado: " + request.getIdEstudiante()));

        String nombreEstudiante = estudiante.getUsuario().getNombres()
                + " " + estudiante.getUsuario().getApellidos();

        Carrera carrera = estudiante.getCarrera();
        String nombreCarrera  = carrera != null ? carrera.getNombre()   : "No especificada";
        String nombreFacultad = (carrera != null && carrera.getFacultad() != null)
                ? carrera.getFacultad().getNombre()
                : "No especificada";

        String modalidad = obtenerModalidad(estudiante.getIdEstudiante());

        String feedbackIa = llamarGroq(request, nombreCarrera, nombreFacultad, modalidad);

        return new RevisionPropuestaIAResponse(
                null,
                request.getTitulo(),
                nombreEstudiante,
                nombreCarrera,
                nombreFacultad,
                modalidad,
                "BORRADOR",
                feedbackIa,
                LocalDateTime.now()
        );
    }

    // ─── Obtener modalidad ────────────────────────────────────────────────

    private String obtenerModalidad(Integer idEstudiante) {
        try {
            return eleccionRepository
                    .findByEstudiante_IdEstudiante(idEstudiante)
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

    private String llamarGroq(RevisionPropuestaPreviaRequest request,
                              String nombreCarrera,
                              String nombreFacultad,
                              String modalidad) {

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + groqApiKey);

        String modo = request.getModo() != null && !request.getModo().isBlank()
                ? request.getModo().trim().toLowerCase()
                : "integral";

        String systemPrompt = construirSystemPrompt(modo, nombreCarrera, nombreFacultad, modalidad);
        String userPrompt   = construirUserPrompt(request, nombreCarrera, modalidad, modo);

        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.3-70b-versatile");
        body.put("messages", List.of(systemMessage, userMessage));
        body.put("temperature", 0.4);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            String raw = restTemplate.postForObject(groqApiUrl, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(raw);
            JsonNode choices = root.path("choices");

            if (!choices.isArray() || choices.isEmpty()) {
                return buildErrorJson("Groq no devolvio choices validos.");
            }

            String content = choices.get(0).path("message").path("content").asText("");
            return extraerJsonDeRespuesta(content);

        } catch (Exception e) {
            e.printStackTrace();
            return buildErrorJson(e.getMessage());
        }
    }

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
        int end   = cleaned.lastIndexOf('}');

        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1).trim();
        }
        return buildErrorJson("La IA no devolvio un JSON reconocible. Respuesta: " + resumir(cleaned));
    }

    private String resumir(String texto) {
        if (texto == null) return "";
        texto = texto.replace("\"", "'");
        return texto.length() > 180 ? texto.substring(0, 180) + "..." : texto;
    }

    // ─── System Prompt — diferenciado por modo ────────────────────────────

    private String construirSystemPrompt(String modo, String nombreCarrera,
                                         String nombreFacultad, String modalidad) {

        boolean esComplexivo = modalidad != null && modalidad.toUpperCase().contains("COMPLEXIVO");

        String contextoModalidad = esComplexivo
                ? "El estudiante opta por EXAMEN COMPLEXIVO. Esta modalidad NO consiste en desarrollar " +
                "un sistema o aplicacion; el trabajo debe demostrar competencias integradas mediante " +
                "analisis, diagnostico y recomendaciones basadas en conocimientos de la carrera. "
                : "El estudiante opta por TRABAJO DE INTEGRACION CURRICULAR. Se espera el desarrollo " +
                "de un producto concreto (sistema, aplicacion, prototipo) que integre los conocimientos " +
                "de la carrera y resuelva un problema real. ";

        String base = "Eres un evaluador academico experto de la " + nombreFacultad + ". " +
                "Evaluas borradores de propuestas de titulacion para la carrera de " + nombreCarrera +
                " bajo la modalidad: " + modalidad + ". " +
                contextoModalidad +
                "Responde UNICAMENTE con JSON valido, sin markdown, sin texto adicional.";

        switch (modo) {
            case "coherencia":
                return base +
                        " ROL ESPECIFICO: Eres un revisor de COHERENCIA INTERNA. " +
                        "Tu unica tarea es verificar si los componentes de la propuesta son consistentes entre si. " +
                        "PREGUNTA CLAVE que debes responder: ¿El titulo describe exactamente lo que plantean los objetivos? " +
                        "¿Los objetivos especificos se derivan logicamente del objetivo general? " +
                        "¿La metodologia es la correcta y suficiente para alcanzar los objetivos planteados? " +
                        "¿Los resultados esperados son consistentes con los objetivos? " +
                        "NO evalues pertinencia a la carrera ni viabilidad de tiempo — solo coherencia interna. " +
                        "En analisis_titulo analiza si el titulo refleja fielmente el contenido. " +
                        "En analisis_objetivos analiza si hay coherencia vertical entre objetivo general y especificos. " +
                        "En analisis_metodologia analiza si la metodologia elegida permite lograr los objetivos. " +
                        "Fortalezas y debilidades deben referirse EXCLUSIVAMENTE a la coherencia interna del documento.";

            case "pertinencia":
                return base +
                        " ROL ESPECIFICO: Eres un revisor de PERTINENCIA ACADEMICA Y DISCIPLINAR. " +
                        "Tu unica tarea es evaluar si el tema es relevante para " + nombreCarrera +
                        " y si es adecuado para la modalidad " + modalidad + ". " +
                        "PREGUNTA CLAVE: ¿Este tema pertenece al campo de " + nombreCarrera + "? " +
                        "¿Los conocimientos y competencias de la carrera son suficientes para desarrollarlo? " +
                        "¿El enfoque del trabajo se alinea con la modalidad " + modalidad + "? " +
                        "¿El area de investigacion es relevante para el perfil de egreso de la carrera? " +
                        "NO evalues si los objetivos son coherentes entre si ni si el proyecto es viable en tiempo. " +
                        "En analisis_titulo analiza si el titulo corresponde al campo de " + nombreCarrera + ". " +
                        "En analisis_objetivos analiza si los objetivos son pertinentes para la carrera y la modalidad. " +
                        "En analisis_metodologia analiza si la metodologia es la apropiada para esta carrera. " +
                        "Fortalezas y debilidades deben referirse EXCLUSIVAMENTE a la pertinencia disciplinar.";

            case "viabilidad":
                return base +
                        " ROL ESPECIFICO: Eres un revisor de VIABILIDAD PRACTICA. " +
                        "Tu unica tarea es evaluar si el proyecto puede completarse en el tiempo tipico " +
                        "de un trabajo de titulacion de pregrado (6 a 12 meses con recursos limitados). " +
                        "PREGUNTA CLAVE: ¿La cantidad de objetivos especificos es razonable para el tiempo disponible? " +
                        "¿Los resultados esperados son alcanzables con los recursos de un estudiante de pregrado? " +
                        "¿La metodologia propuesta puede ejecutarse completamente en ese tiempo? " +
                        "¿El alcance del proyecto es demasiado amplio, demasiado reducido, o apropiado? " +
                        "¿Existen dependencias externas criticas que puedan bloquear el proyecto? " +
                        "NO evalues coherencia entre objetivos ni pertinencia a la carrera. " +
                        "En analisis_titulo analiza si el alcance implico en el titulo es realista. " +
                        "En analisis_objetivos analiza si la cantidad y complejidad de objetivos es viable. " +
                        "En analisis_metodologia analiza si la metodologia es ejecutable en el tiempo disponible. " +
                        "Fortalezas y debilidades deben referirse EXCLUSIVAMENTE a la viabilidad practica.";

            case "integral":
            default:
                return base +
                        " ROL ESPECIFICO: Eres un revisor INTEGRAL. " +
                        "Tu tarea es evaluar todos los aspectos de la propuesta de forma holistica: " +
                        "(1) COHERENCIA: ¿titulo, objetivos, metodologia y resultados son consistentes? " +
                        "(2) PERTINENCIA: ¿el tema es adecuado para " + nombreCarrera + " y la modalidad " + modalidad + "? " +
                        "(3) VIABILIDAD: ¿el proyecto es realizable en tiempo y recursos de pregrado? " +
                        "(4) CALIDAD: ¿el planteamiento del problema esta bien fundamentado? " +
                        "(5) SOLIDEZ: ¿los objetivos son SMART (especificos, medibles, alcanzables)? " +
                        "Proporciona fortalezas concretas, debilidades reales y sugerencias accionables. " +
                        "El puntaje debe reflejar una evaluacion honesta y equilibrada de todos estos criterios.";
        }
    }

    // ─── User Prompt — incluye instruccion de enfoque segun modo ─────────

    private String construirUserPrompt(RevisionPropuestaPreviaRequest request,
                                       String nombreCarrera, String modalidad, String modo) {
        StringBuilder sb = new StringBuilder();

        // Encabezado con el modo activo — refuerza el enfoque
        sb.append("=== TIPO DE ANALISIS SOLICITADO: ");
        switch (modo) {
            case "coherencia":
                sb.append("COHERENCIA INTERNA ===\n");
                sb.append("Analiza UNICAMENTE si los componentes de la propuesta son consistentes entre si.\n");
                sb.append("Ignora si el tema es pertinente a la carrera o si el tiempo es suficiente.\n\n");
                break;
            case "pertinencia":
                sb.append("PERTINENCIA A LA CARRERA ===\n");
                sb.append("Analiza UNICAMENTE si el tema y enfoque son adecuados para ").append(nombreCarrera);
                sb.append(" y para la modalidad ").append(modalidad).append(".\n");
                sb.append("Ignora la coherencia interna entre objetivos y la viabilidad de tiempo.\n\n");
                break;
            case "viabilidad":
                sb.append("VIABILIDAD DEL PROYECTO ===\n");
                sb.append("Analiza UNICAMENTE si el proyecto es ejecutable en 6 a 12 meses por un estudiante de pregrado.\n");
                sb.append("Ignora la coherencia interna y la pertinencia a la carrera.\n\n");
                break;
            default:
                sb.append("EVALUACION INTEGRAL ===\n");
                sb.append("Analiza todos los aspectos: coherencia, pertinencia, viabilidad y calidad general.\n\n");
        }

        sb.append("=== CONTEXTO ACADEMICO ===\n");
        sb.append("CARRERA: ").append(nombreCarrera).append("\n");
        sb.append("MODALIDAD: ").append(modalidad).append("\n\n");

        sb.append("=== BORRADOR DE PROPUESTA ===\n");
        sb.append("TITULO: ").append(safe(request.getTitulo())).append("\n\n");
        sb.append("TEMA DE INVESTIGACION: ").append(safe(request.getTemaInvestigacion())).append("\n\n");
        sb.append("PLANTEAMIENTO DEL PROBLEMA:\n").append(safe(request.getPlanteamientoProblema())).append("\n\n");
        sb.append("OBJETIVO GENERAL:\n").append(safe(request.getObjetivosGenerales())).append("\n\n");
        sb.append("OBJETIVOS ESPECIFICOS:\n").append(safe(request.getObjetivosEspecificos())).append("\n\n");
        sb.append("MARCO TEORICO:\n").append(safe(request.getMarcoTeorico())).append("\n\n");
        sb.append("METODOLOGIA:\n").append(safe(request.getMetodologia())).append("\n\n");
        sb.append("RESULTADOS ESPERADOS:\n").append(safe(request.getResultadosEsperados())).append("\n\n");
        sb.append("BIBLIOGRAFIA:\n").append(safe(request.getBibliografia())).append("\n\n");

        if (request.getInstruccionAdicional() != null && !request.getInstruccionAdicional().isBlank()) {
            sb.append("NOTA DEL ESTUDIANTE: ").append(request.getInstruccionAdicional()).append("\n\n");
        }

        // Instruccion final reforzada con el modo
        sb.append("=== INSTRUCCION FINAL ===\n");
        switch (modo) {
            case "coherencia":
                sb.append("Responde evaluando SOLO la coherencia interna: ");
                sb.append("¿el titulo, objetivos, metodologia y resultados forman un conjunto consistente? ");
                sb.append("El puntaje_estimado debe reflejar el nivel de coherencia interna (0-100). ");
                sb.append("pertinencia_carrera puede ser MEDIA si no tienes suficiente informacion para determinarlo.\n\n");
                break;
            case "pertinencia":
                sb.append("Responde evaluando SOLO la pertinencia: ");
                sb.append("¿el tema es relevante para ").append(nombreCarrera);
                sb.append(" y apropiado para la modalidad ").append(modalidad).append("? ");
                sb.append("El puntaje_estimado debe reflejar el nivel de pertinencia (0-100). ");
                sb.append("pertinencia_carrera es el campo mas importante de tu respuesta.\n\n");
                break;
            case "viabilidad":
                sb.append("Responde evaluando SOLO la viabilidad: ");
                sb.append("¿puede un estudiante de pregrado completar este proyecto en 6 a 12 meses? ");
                sb.append("El puntaje_estimado debe reflejar que tan viable es el proyecto (0-100). ");
                sb.append("pertinencia_carrera puede ser MEDIA si no tienes suficiente informacion.\n\n");
                break;
            default:
                sb.append("Responde con una evaluacion integral y equilibrada de todos los aspectos. ");
                sb.append("El puntaje_estimado debe reflejar la calidad general de la propuesta (0-100).\n\n");
        }

        sb.append("Responde UNICAMENTE en JSON valido con este formato, sin texto extra:\n");
        sb.append("{\n");
        sb.append("  \"estado_evaluacion\": \"APROBABLE|REQUIERE_AJUSTES|RECHAZABLE\",\n");
        sb.append("  \"puntaje_estimado\": 0,\n");
        sb.append("  \"pertinencia_carrera\": \"ALTA|MEDIA|BAJA\",\n");
        sb.append("  \"analisis_titulo\": \"analisis especifico segun el modo solicitado\",\n");
        sb.append("  \"analisis_objetivos\": \"analisis especifico segun el modo solicitado\",\n");
        sb.append("  \"analisis_metodologia\": \"analisis especifico segun el modo solicitado\",\n");
        sb.append("  \"fortalezas\": [\"fortaleza 1\", \"fortaleza 2\"],\n");
        sb.append("  \"debilidades\": [\"debilidad 1\", \"debilidad 2\"],\n");
        sb.append("  \"sugerencias_mejora\": [\"sugerencia 1\", \"sugerencia 2\"],\n");
        sb.append("  \"mensaje_estudiante\": \"retroalimentacion directa y constructiva segun el tipo de analisis\"\n");
        sb.append("}");

        return sb.toString();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private String safe(String valor) {
        return valor != null && !valor.isBlank() ? valor : "(no proporcionado)";
    }

    private String buildErrorJson(String errorMsg) {
        String safeError = errorMsg == null ? "Error desconocido" : errorMsg.replace("\"", "'");
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