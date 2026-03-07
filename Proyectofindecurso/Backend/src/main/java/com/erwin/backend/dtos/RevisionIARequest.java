package com.erwin.backend.dtos;

public class RevisionIARequest {
    private String modo;
    private String promptPersonalizado;

    public String getModo() {
        return modo;
    }

    public void setModo(String modo) {
        this.modo = modo;
    }

    public String getPromptPersonalizado() {
        return promptPersonalizado;
    }

    public void setPromptPersonalizado(String promptPersonalizado) {
        this.promptPersonalizado = promptPersonalizado;
    }
}