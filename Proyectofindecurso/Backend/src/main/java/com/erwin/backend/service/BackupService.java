package com.erwin.backend.service;

import com.erwin.backend.config.DbContextHolder;
import com.erwin.backend.dtos.BackupDto;
import com.erwin.backend.entities.BackupConfiguracion;
import com.erwin.backend.entities.BackupHistorial;
import com.erwin.backend.repository.BackupConfiguracionRepository;
import com.erwin.backend.repository.BackupHistorialRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {

    private final BackupConfiguracionRepository configRepo;
    private final BackupHistorialRepository     historialRepo;
    private final GoogleDriveService            driveService;

    // URL de la BD para extraer host, puerto y nombre
    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    // Ruta del binario pg_dump — configurable en application.properties
    @Value("${backup.pgdump.path:pg_dump}")
    private String pgDumpPath;

    public BackupService(BackupConfiguracionRepository configRepo,
                         BackupHistorialRepository historialRepo,
                         GoogleDriveService driveService) {
        this.configRepo    = configRepo;
        this.historialRepo = historialRepo;
        this.driveService  = driveService;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CONFIGURACIÓN
    // ──────────────────────────────────────────────────────────────────────────

    public BackupDto.ConfiguracionDto obtenerConfiguracion() {
        return toConfigDto(obtenerConfigEntity());
    }

    public BackupDto.ConfiguracionDto guardarConfiguracion(BackupDto.ConfiguracionDto dto) {
        BackupConfiguracion cfg = obtenerConfigEntity();

        if (dto.getRutaLocal()        != null) cfg.setRutaLocal(dto.getRutaLocal());
        if (dto.getCantidadMaxima()   != null) cfg.setCantidadMaxima(dto.getCantidadMaxima());
        if (dto.getComprimir()        != null) cfg.setComprimir(dto.getComprimir());
        if (dto.getGuardarEnDrive()   != null) cfg.setGuardarEnDrive(dto.getGuardarEnDrive());
        if (dto.getDriveFolderId()    != null) cfg.setDriveFolderId(dto.getDriveFolderId());
        if (dto.getTipoRespaldo()     != null) cfg.setTipoRespaldo(dto.getTipoRespaldo());
        if (dto.getProgramadoActivo() != null) cfg.setProgramadoActivo(dto.getProgramadoActivo());

        // horaProgramada llega como "HH:mm" desde Angular
        if (dto.getHoraProgramada() != null && !dto.getHoraProgramada().isBlank()) {
            cfg.setHoraProgramada(LocalTime.parse(dto.getHoraProgramada()));
        } else {
            cfg.setHoraProgramada(null);
        }

        return toConfigDto(configRepo.save(cfg));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // HISTORIAL
    // ──────────────────────────────────────────────────────────────────────────

    public List<BackupDto.HistorialDto> listarHistorial() {
        return historialRepo.findAllByOrderByFechaCreacionDesc()
                .stream()
                .map(this::toHistorialDto)
                .collect(Collectors.toList());
    }

    public void eliminarHistorial(Integer id) {
        BackupHistorial h = historialRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro de respaldo no encontrado: " + id));

        if (h.getRutaCompleta() != null) {
            borrarArchivoFisico(Path.of(h.getRutaCompleta()));
        }
        if (Boolean.TRUE.equals(h.getEnDrive()) && h.getDriveFileId() != null) {
            driveService.eliminarArchivo(h.getDriveFileId());
        }
        historialRepo.deleteById(id);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // EJECUCIÓN DEL RESPALDO
    // ──────────────────────────────────────────────────────────────────────────

    public BackupDto.EjecucionResultDto ejecutarRespaldo() {

        // ── CLAVE: leer las credenciales del usuario logueado desde el ThreadLocal ──
        // Cuando el admin llama a este endpoint, DbSessionFilter ya puso
        // su username_db ("app_admin") y su contraseña en DbContextHolder.
        // Así usamos exactamente el mismo usuario de BD que usa todo el sistema.
        String dbUser = DbContextHolder.getUser();
        String dbPass = DbContextHolder.getPass();

        if (dbUser == null || dbUser.isBlank()) {
            return new BackupDto.EjecucionResultDto(
                    false,
                    "No hay sesión activa. Debes estar logueado para ejecutar un respaldo.",
                    null
            );
        }

        BackupConfiguracion cfg      = obtenerConfigEntity();
        BackupHistorial     historial = new BackupHistorial();
        historial.setTipoRespaldo(cfg.getTipoRespaldo());

        try {
            // 1) Preparar directorio destino
            Path dirDestino = Path.of(cfg.getRutaLocal());
            Files.createDirectories(dirDestino);

            // 2) Nombre del archivo con timestamp
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String dbName    = extraerNombreBd(datasourceUrl);
            String tipo      = cfg.getTipoRespaldo().toLowerCase();
            String nombreSql = "backup_" + dbName + "_" + tipo + "_" + timestamp + ".sql";
            Path   archivoSql = dirDestino.resolve(nombreSql);

            // 3) Ejecutar pg_dump con las credenciales del usuario logueado
            ejecutarPgDump(dbName, archivoSql, cfg.getTipoRespaldo(), dbUser, dbPass);

            // 4) Comprimir si está configurado
            Path archivoFinal;
            if (Boolean.TRUE.equals(cfg.getComprimir())) {
                archivoFinal = comprimirEnZip(archivoSql, dirDestino, timestamp, dbName, tipo);
                borrarArchivoFisico(archivoSql); // borrar .sql original
            } else {
                archivoFinal = archivoSql;
            }

            // 5) Registrar en historial
            historial.setNombreArchivo(archivoFinal.getFileName().toString());
            historial.setRutaCompleta(archivoFinal.toAbsolutePath().toString());
            historial.setTamanioBytes(Files.size(archivoFinal));
            historial.setEstado("EXITOSO");

            // 6) Subir a Drive si está configurado
            if (Boolean.TRUE.equals(cfg.getGuardarEnDrive()) && cfg.getDriveFolderId() != null) {
                try {
                    String driveId = driveService.subirArchivo(archivoFinal, cfg.getDriveFolderId());
                    historial.setEnDrive(true);
                    historial.setDriveFileId(driveId);
                } catch (Exception driveEx) {
                    // Drive falló pero el backup local quedó — solo avisar
                    historial.setMensajeError(
                            "Backup local OK. Error al subir a Drive: " + driveEx.getMessage()
                    );
                }
            }

            // 7) Guardar en BD
            BackupHistorial guardado = historialRepo.save(historial);

            // 8) Rotar backups viejos según cantidad máxima
            rotarBackupsViejos(cfg);

            return new BackupDto.EjecucionResultDto(
                    true, "Respaldo completado exitosamente", toHistorialDto(guardado)
            );

        } catch (Exception e) {
            historial.setEstado("FALLIDO");
            historial.setMensajeError(e.getMessage());
            historial.setNombreArchivo(
                    "ERROR_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            );
            historialRepo.save(historial);
            return new BackupDto.EjecucionResultDto(
                    false, "Error al ejecutar respaldo: " + e.getMessage(), toHistorialDto(historial)
            );
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DESCARGA
    // ──────────────────────────────────────────────────────────────────────────

    public Path obtenerRutaParaDescarga(Integer id) {
        BackupHistorial h = historialRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Respaldo no encontrado"));
        Path ruta = Path.of(h.getRutaCompleta());
        if (!Files.exists(ruta)) {
            throw new RuntimeException("El archivo físico ya no existe en: " + ruta);
        }
        return ruta;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // INTERNOS — pg_dump
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Ejecuta pg_dump usando las credenciales del usuario logueado (app_admin).
     * Esas credenciales vienen del DbContextHolder, que las recibió del JWT.
     */
    private void ejecutarPgDump(String dbName, Path destino, String tipoRespaldo,
                                String dbUser, String dbPass)
            throws IOException, InterruptedException {

        // Extraer host y puerto del JDBC URL
        // Formato: jdbc:postgresql://host:port/dbname
        String host = "localhost";
        String port = "5432";
        String sinJdbc = datasourceUrl.replace("jdbc:postgresql://", "");
        String[] partes = sinJdbc.split("/");
        if (partes.length > 0) {
            String[] hostPort = partes[0].split(":");
            host = hostPort[0];
            if (hostPort.length > 1) port = hostPort[1];
        }

        ProcessBuilder pb;

        switch (tipoRespaldo.toUpperCase()) {

            case "DIFERENCIAL":
                // Solo datos — sin estructura (asume que ya existe el esquema)
                pb = new ProcessBuilder(
                        pgDumpPath,
                        "-h", host, "-p", port,
                        "-U", dbUser,
                        "--data-only",
                        "--format=plain",
                        "-f", destino.toAbsolutePath().toString(),
                        dbName
                );
                break;

            case "INCREMENTAL":
                // Esquema + datos por secciones
                pb = new ProcessBuilder(
                        pgDumpPath,
                        "-h", host, "-p", port,
                        "-U", dbUser,
                        "--format=plain",
                        "--section=pre-data",
                        "--section=data",
                        "-f", destino.toAbsolutePath().toString(),
                        dbName
                );
                break;

            default: // COMPLETO
                pb = new ProcessBuilder(
                        pgDumpPath,
                        "-h", host, "-p", port,
                        "-U", dbUser,
                        "--format=plain",
                        "--clean",
                        "--if-exists",
                        "-f", destino.toAbsolutePath().toString(),
                        dbName
                );
        }

        // Pasar la contraseña por variable de entorno (seguro, no aparece en logs)
        pb.environment().put("PGPASSWORD", dbPass);
        pb.redirectErrorStream(true);

        Process proceso = pb.start();

        // Capturar salida por si falla
        StringBuilder salida = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(proceso.getInputStream()))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                salida.append(linea).append("\n");
            }
        }

        int exitCode = proceso.waitFor();
        if (exitCode != 0) {
            throw new IOException(
                    "pg_dump terminó con código " + exitCode + ":\n" + salida
            );
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // INTERNOS — Compresión ZIP
    // ──────────────────────────────────────────────────────────────────────────

    private Path comprimirEnZip(Path archivoSql, Path directorio,
                                String timestamp, String dbName, String tipo)
            throws IOException {

        String nombreZip = "backup_" + dbName + "_" + tipo + "_" + timestamp + ".zip";
        Path   zipPath   = directorio.resolve(nombreZip);

        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream(zipPath.toFile()));
             FileInputStream fis = new FileInputStream(archivoSql.toFile())) {

            zos.putNextEntry(new ZipEntry(archivoSql.getFileName().toString()));
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
        }
        return zipPath;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // INTERNOS — Rotación
    // ──────────────────────────────────────────────────────────────────────────

    private void rotarBackupsViejos(BackupConfiguracion cfg) {
        int max = cfg.getCantidadMaxima() != null ? cfg.getCantidadMaxima() : 10;
        List<BackupHistorial> exitosos = historialRepo.findExitososMasAntiguosPrimero();

        while (exitosos.size() > max) {
            BackupHistorial viejo = exitosos.remove(0);
            if (viejo.getRutaCompleta() != null) {
                borrarArchivoFisico(Path.of(viejo.getRutaCompleta()));
            }
            if (Boolean.TRUE.equals(viejo.getEnDrive()) && viejo.getDriveFileId() != null) {
                driveService.eliminarArchivo(viejo.getDriveFileId());
            }
            historialRepo.delete(viejo);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // INTERNOS — Utilidades
    // ──────────────────────────────────────────────────────────────────────────

    private BackupConfiguracion obtenerConfigEntity() {
        return configRepo.findAll().stream().findFirst().orElseGet(() -> {
            BackupConfiguracion nueva = new BackupConfiguracion();
            nueva.setRutaLocal("C:/respaldos");
            nueva.setCantidadMaxima(10);
            nueva.setComprimir(true);
            nueva.setTipoRespaldo("COMPLETO");
            return configRepo.save(nueva);
        });
    }

    private String extraerNombreBd(String jdbcUrl) {
        try {
            String sin = jdbcUrl.replace("jdbc:postgresql://", "");
            String[] partes = sin.split("/");
            return partes[partes.length - 1].split("\\?")[0];
        } catch (Exception e) {
            return "database";
        }
    }

    private void borrarArchivoFisico(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("⚠️ No se pudo borrar: " + path + " — " + e.getMessage());
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private BackupDto.ConfiguracionDto toConfigDto(BackupConfiguracion e) {
        BackupDto.ConfiguracionDto d = new BackupDto.ConfiguracionDto();
        d.setId(e.getId());
        d.setRutaLocal(e.getRutaLocal());
        d.setCantidadMaxima(e.getCantidadMaxima());
        d.setComprimir(e.getComprimir());
        d.setGuardarEnDrive(e.getGuardarEnDrive());
        d.setDriveFolderId(e.getDriveFolderId());
        d.setTipoRespaldo(e.getTipoRespaldo());
        d.setProgramadoActivo(e.getProgramadoActivo());
        d.setHoraProgramada(e.getHoraProgramada() != null
                ? e.getHoraProgramada().format(DateTimeFormatter.ofPattern("HH:mm"))
                : null);
        return d;
    }

    private BackupDto.HistorialDto toHistorialDto(BackupHistorial e) {
        BackupDto.HistorialDto d = new BackupDto.HistorialDto();
        d.setId(e.getId());
        d.setNombreArchivo(e.getNombreArchivo());
        d.setRutaCompleta(e.getRutaCompleta());
        d.setTipoRespaldo(e.getTipoRespaldo());
        d.setTamanioBytes(e.getTamanioBytes());
        d.setEnDrive(e.getEnDrive());
        d.setDriveFileId(e.getDriveFileId());
        d.setEstado(e.getEstado());
        d.setMensajeError(e.getMensajeError());
        d.setFechaCreacion(e.getFechaCreacion());
        return d;
    }
}