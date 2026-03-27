package com.erwin.backend;

import com.erwin.backend.security.CryptoUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BackendApplication {
    public static void main(String[] args) {
        // ── DEBUG TEMPORAL: desencriptar password_db_encrypted del admin ──
        try {
            String encrypted = "9LYHiLNE+qvMJXEyQ2z/Kg==";
            String decrypted = CryptoUtil.decrypt(encrypted);
            System.out.println("[DEBUG] Contraseña desencriptada de app_admin: " + decrypted);
        } catch (Exception e) {
            System.err.println("[DEBUG ERROR] No se pudo desencriptar: " + e.getMessage());
        }
        // ─────────────────────────────────────────────────────────────────
        SpringApplication.run(BackendApplication.class, args);
    }
}
