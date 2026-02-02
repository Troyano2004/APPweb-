package com.erwin.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HolaController {

    @GetMapping("/test")
    public String prueba() {
        return "¡Spring Boot está vivo y funcionando en la terminal de Erwin!";
    }
}
