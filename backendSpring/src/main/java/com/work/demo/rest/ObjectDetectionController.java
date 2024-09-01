package com.work.demo.rest;

import com.work.demo.rest.dto.ObjectDetectionResult;
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
    @CrossOrigin(origins = "http://localhost:4200")
    @PostMapping("/detect")
    public List<ObjectDetectionContainer> detectObjects(@RequestParam("image") MultipartFile image) {
        List<ObjectDetectionResult> objectList = new ArrayList<>();
      //  objectList.add(new ObjectDetectionResult("Person", 0.95f));
        //  objectList.add(new ObjectDetectionResult("Dog", 0.90f));
        //  objectList.add(new ObjectDetectionResult("Car", 0.85f));
        // objectList.add(new ObjectDetectionResult("Bicycle", 0.80f));
        //  objectList.add(new ObjectDetectionResult("Cat", 0.75f));

        //ObjectDetectionContainer container = new ObjectDetectionContainer(objectList,5);

        List<ObjectDetectionContainer> results = performObjectDetection(image);//new ArrayList<>();
        //results.add(container);
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

        return obj.performObjectDetection(image);
    }
}

