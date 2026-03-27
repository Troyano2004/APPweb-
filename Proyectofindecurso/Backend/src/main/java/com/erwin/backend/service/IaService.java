package com.erwin.backend.service;

import com.erwin.backend.entities.IaEjemplo;
import com.erwin.backend.repository.IaEjemploRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class IaService {




    @Value("${gemini.api.key}")
    private String GEMINI_API_KEY;
    @Value("${gemini.url}")
    private String GEMINI_URL;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient   client = HttpClient.newHttpClient();
    private final IaEjemploRepository iaEjemploRepo;

    public IaService(IaEjemploRepository iaEjemploRepo) {
        this.iaEjemploRepo = iaEjemploRepo;
    }

    public String analizarSeccion(String seccion, String contenido, Integer idEstudiante) {
        List<IaEjemplo> ejemplos = iaEjemploRepo.findTop10ByIdEstudiante(idEstudiante);
        String prompt = construirPrompt(seccion, contenido, ejemplos);
        return llamarGemini(prompt);

    }

    private String construirPrompt(String seccion, String contenido, List<IaEjemplo>Ejemplos) {

        String criterios = switch (seccion) {
            case "portada" -> """
    Evalúa la sección PORTADA del anteproyecto según el Reglamento UTEQ (Art. 16, 46, 47):
    
    TÍTULO:
    - Debe ser claro, específico y coherente con el tema de investigación
    - Debe reflejar el objeto de estudio y el alcance del trabajo
    - No debe ser demasiado largo ni demasiado genérico
    - Debe estar relacionado con las líneas de investigación de la carrera
    
    TEMA DE INVESTIGACIÓN:
    - Debe ser relevante y pertinente para la carrera
    - Debe estar relacionado con las líneas de investigación institucionales
    - Debe ser innovador y aportar al conocimiento del área
    - Debe guardar coherencia con el título del trabajo
    """;

            case "introduccion" -> """
    Evalúa la sección INTRODUCCIÓN del anteproyecto según el Reglamento UTEQ (Art. 16, 42, 46):
    
    PLANTEAMIENTO DEL PROBLEMA:
    - Debe tener contextualización clara del problema (situación actual)
    - Debe tener delimitación del problema (dónde, cuándo, con quién)
    - Debe incluir la formulación del problema (pregunta de investigación)
    - Debe justificar la relevancia e importancia de resolver el problema
    - Mínimo 3 párrafos bien estructurados
    
    OBJETIVO GENERAL:
    - Debe iniciar con verbo en infinitivo (Desarrollar, Implementar, Analizar, Diseñar...)
    - Debe ser uno solo y responder directamente al problema planteado
    - Debe ser alcanzable dentro del período académico
    - Debe ser coherente con el título y el tema de investigación
    
    OBJETIVOS ESPECÍFICOS:
    - Cada objetivo debe iniciar con verbo en infinitivo diferente
    - Deben ser mínimo 3 objetivos específicos
    - Deben ser medibles, alcanzables y relacionados con el objetivo general
    - Juntos deben cubrir el alcance completo del objetivo general
    - No deben ser actividades sino resultados esperados
    """;

            case "marco" -> """
    Evalúa el MARCO TEÓRICO según el Reglamento UTEQ (Art. 16, 42):
    - Debe incluir fundamentación conceptual del tema
    - Debe citar fuentes bibliográficas relevantes y actualizadas
    - Debe estar organizado de forma lógica y coherente
    - Debe sustentarse en trabajos previos relacionados al tema
    - Cuando corresponda debe incluir fundamentación legal
    """;

            case "resultados" -> """
    Evalúa la sección METODOLOGÍA Y RESULTADOS según el Reglamento UTEQ (Art. 16, 42, 45):
    
    METODOLOGÍA:
    - Debe especificar el tipo de investigación (descriptiva, exploratoria, aplicada...)
    - Debe indicar el enfoque (cuantitativo, cualitativo o mixto)
    - Debe describir las técnicas e instrumentos de recolección de datos
    - Debe ser coherente con los objetivos planteados
    - Para proyectos tecnológicos debe incluir fases de desarrollo
    
    RESULTADOS ESPERADOS:
    - Deben estar directamente vinculados a cada objetivo específico
    - Deben ser concretos, medibles y verificables
    - Deben demostrar el aporte e impacto del trabajo
    - Deben ser alcanzables con la metodología propuesta
    """;

            case "bibliografia" -> """
    Evalúa la BIBLIOGRAFÍA según el Reglamento UTEQ (Art. 46, 58):
    - Para Ciencias de la Ingeniería debe usar formato IEEE o APA
    - Debe tener mínimo 10 referencias bibliográficas
    - Al menos el 60%% debe corresponder a los últimos 5 años
    - Las referencias deben ser relevantes y pertinentes al tema
    - No se aceptan fuentes sin respaldo académico (Wikipedia, blogs sin autor)
    """;

            default -> "Evalúa esta sección del anteproyecto de titulación de la UTEQ.";
        };
        StringBuilder historial = new StringBuilder();
        if(!Ejemplos.isEmpty()) {
            historial.append("HISTORIAL DE REVISIONES DE ESTE ESTUDIANTE:\n"); // ← falta esto
            Ejemplos.forEach(ej -> {
                historial.append("- Fuente: ").append(ej.getFuente()).append("\n");
                historial.append("  Decisión/Cumplimiento: ").append(ej.getDecision()).append("\n");
                historial.append("  Observación del DT1: ").append(ej.getObservacion()).append("\n\n");

            });
            historial.append("IMPORTANTE: Toma en cuenta este historial. Si el DT1 ha observado algo repetidamente, refuérzalo en tus sugerencias. Si el estudiante ya mejoró algo que antes fue observado, reconócelo.\n\n");


        }

        return String.format("""
             Eres un evaluador académico experto del Sistema de Titulación de la UTEQ.
                    Tu rol es ayudar al estudiante a MEJORAR su anteproyecto, NO escribirlo por él.
            
                    %s
                    CRITERIOS A EVALUAR:
                    %s
            
                    CONTENIDO ESCRITO POR EL ESTUDIANTE:
                    %s
            
                    Proporciona tu análisis en este formato exacto:
            
                    ESTADO: [BIEN / NECESITA MEJORAS / INCOMPLETO]
            
                    OBSERVACIONES:
                    • [observación 1]
                    • [observación 2]
                    • [observación 3]
            
                    SUGERENCIA PRINCIPAL: [la mejora más importante]
                    """, historial.toString(), criterios, contenido);
    }

    private String llamarGemini(String prompt) {
        try {

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "maxOutputTokens", 800,
                            "temperature", 0.0,
                            "thinkingConfig", Map.of(
                                    "thinkingBudget", 0
                            )
                    )
            );

            String json = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_URL + "?key=" + GEMINI_API_KEY))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            // Mostrar respuesta en consola
            System.out.println("Respuesta Gemini:");
            System.out.println(response.body());

            Map<?, ?> resp = mapper.readValue(response.body(), Map.class);

            // validar error
            if (resp.containsKey("error")) {
                Map<?, ?> error = (Map<?, ?>) resp.get("error");
                return "Error IA: " + error.get("message");
            }

            // validar candidates
            if (!resp.containsKey("candidates")) {
                return "Gemini no devolvió respuesta válida";
            }

            List<?> candidates = (List<?>) resp.get("candidates");

            if (candidates.isEmpty()) {
                return "Gemini no generó contenido";
            }

            Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) candidate.get("content");
            List<?> parts = (List<?>) content.get("parts");
            Map<?, ?> part = (Map<?, ?>) parts.get(0);

            return (String) part.get("text");

        } catch (Exception e) {
            return "ERROR_IA: " + e.getMessage();
        }
    }

}