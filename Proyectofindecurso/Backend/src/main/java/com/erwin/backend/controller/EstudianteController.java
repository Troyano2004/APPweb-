package com.erwin.backend.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


import com.erwin.backend.entities.Estudiante;
import com.erwin.backend.repository.EstudianteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/estudiantes")
@CrossOrigin(origins = "http://localhost:4200") // Permite que Angular consuma este servicio
public class EstudianteController {

    @Autowired
    private EstudianteRepository estudianteRepository;

    @GetMapping
    public List<Estudiante> listarEstudiantes() {
        // Esto hace la consulta "SELECT * FROM estudiante" automáticamente
        return estudianteRepository.findAll();
    }
    @PostMapping
    public Estudiante crear(@RequestBody Estudiante estudiante) {
        return estudianteRepository.save(estudiante);
    }
    @PostMapping("/crear-completo")
    public Estudiante crearCompleto(@RequestBody Estudiante e) {
        return estudianteRepository.save(e);
    }



}