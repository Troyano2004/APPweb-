// Proyectofindecurso/Backend/src/main/java/com/erwin/backend/dtos/ImageUploadResponseDto.java
package com.erwin.backend.dtos;

public class ImageUploadResponseDto {
    private String url;
    private String fileName;

    public ImageUploadResponseDto() {
    }

    public ImageUploadResponseDto(String url, String fileName) {
        this.url = url;
        this.fileName = fileName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
