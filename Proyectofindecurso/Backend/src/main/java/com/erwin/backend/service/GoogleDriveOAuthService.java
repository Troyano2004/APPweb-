package com.erwin.backend.service;

import com.erwin.backend.entities.BackupDestination;
import com.erwin.backend.repository.BackupDestinationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleDriveOAuthService {


    private static final String CLIENT_ID     = "4";
    private static final String CLIENT_SECRET = "";
    private static final String REDIRECT_URI  = "http://localhost:8080/api/backup/oauth/callback";
    private static final String TOKEN_URL     = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL  = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String SCOPE         = "https://www.googleapis.com/auth/drive "
            + "https://www.googleapis.com/auth/userinfo.email";

    private final BackupEncryptionUtil       encryption;
    private final BackupDestinationRepository destinationRepo;
    private final RestTemplate               restTemplate;

    // ── Generar URL de autorización ────────────────────────────────────────────

    public String generarUrlAutorizacion(Long destinationId) {
        return "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id="     + CLIENT_ID +
                "&redirect_uri="  + REDIRECT_URI +
                "&response_type=code" +
                "&scope="         + SCOPE.replace(" ", "%20") +
                "&access_type=offline" +
                "&prompt=consent" +
                "&state="         + destinationId;   // usamos state para saber qué destino actualizar
    }

    // ── Intercambiar código por tokens ─────────────────────────────────────────

    public TokenResult intercambiarCodigo(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code",          code);
        params.add("client_id",     CLIENT_ID);
        params.add("client_secret", CLIENT_SECRET);
        params.add("redirect_uri",  REDIRECT_URI);
        params.add("grant_type",    "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response = restTemplate.exchange(
                TOKEN_URL, HttpMethod.POST,
                new HttpEntity<>(params, headers), Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Error obteniendo tokens de Google");
        }

        Map<String, Object> body = response.getBody();
        String accessToken  = (String) body.get("access_token");
        String refreshToken = (String) body.get("refresh_token");

        // Obtener email del usuario
        String email = obtenerEmail(accessToken);

        return new TokenResult(accessToken, refreshToken, email);
    }

    // ── Obtener access token desde refresh token ───────────────────────────────

    public String obtenerAccessToken(String refreshTokenEnc) {
        String refreshToken = encryption.decrypt(refreshTokenEnc);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("refresh_token", refreshToken);
        params.add("client_id",     CLIENT_ID);
        params.add("client_secret", CLIENT_SECRET);
        params.add("grant_type",    "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response = restTemplate.exchange(
                TOKEN_URL, HttpMethod.POST,
                new HttpEntity<>(params, headers), Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Error renovando access token de Google");
        }

        return (String) response.getBody().get("access_token");
    }

    // ── Guardar refresh token en el destino ────────────────────────────────────

    public void guardarTokenEnDestino(Long destinationId, TokenResult tokens) {
        BackupDestination dest = destinationRepo.findById(destinationId)
                .orElseThrow(() -> new RuntimeException("Destino no encontrado: " + destinationId));

        dest.setGdriveCuenta(tokens.email());
        dest.setGdriveRefreshTokenEnc(encryption.encrypt(tokens.refreshToken()));
        destinationRepo.save(dest);
        log.info("Refresh token de Google Drive guardado para destino={}", destinationId);
    }

    // ── Verificar si el destino ya está conectado ──────────────────────────────

    public boolean estaConectado(BackupDestination dest) {
        return dest.getGdriveRefreshTokenEnc() != null
                && !dest.getGdriveRefreshTokenEnc().isBlank();
    }

    // ── Obtener email del usuario ──────────────────────────────────────────────

    private String obtenerEmail(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            ResponseEntity<Map> resp = restTemplate.exchange(
                    USERINFO_URL, HttpMethod.GET,
                    new HttpEntity<>(headers), Map.class);
            if (resp.getBody() != null) {
                return (String) resp.getBody().get("email");
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener email de Google: {}", e.getMessage());
        }
        return "cuenta-google";
    }

    public record TokenResult(String accessToken, String refreshToken, String email) {}
}