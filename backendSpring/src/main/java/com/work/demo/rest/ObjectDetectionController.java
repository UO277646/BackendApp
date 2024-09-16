package com.work.demo.rest;

import com.work.demo.rest.dto.ObjectDetectionResult;
import com.work.demo.service.DeteccionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.work.demo.rest.dto.ObjectDetectionContainer;
import com.work.demo.rest.dto.ObjectPruebaDto;
import com.work.demo.service.ObjectDetectionService;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ObjectDetectionController {
    @Autowired
    private ObjectDetectionService obj;
    @Autowired
    private DeteccionService deteccionService;
    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/detect")
    public List<ObjectDetectionContainer> detectObjects(@RequestParam("image") MultipartFile image) {
        List<ObjectDetectionResult> objectList = new ArrayList<>();
        if(deteccionService.checkToday()){
            throw new RuntimeException("Hoy ya se ha subido foto");
        }
        List<ObjectDetectionContainer> results = performObjectDetection(image);
        return results;
    }
    @GetMapping("/list")
    public List<ObjectPruebaDto> detectObjects() {
        List<ObjectPruebaDto> l=new ArrayList<>();
        ObjectPruebaDto o=new ObjectPruebaDto();
        o.setNombre("Juan");
        o.setApellido("Domingo");
        l.add(o);
        return l;
    }
    private List<ObjectDetectionContainer> performObjectDetection(MultipartFile image) {
        // Aquí es donde se realizaría la detección de objetos utilizando ONNX Runtime Java
        // Procesar la imagen, cargar el modelo ONNX y obtener los resultados de detección

        return obj.performConeDetection(image);
    }
}

