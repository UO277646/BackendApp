package com.work.demo.rest;

import com.work.demo.rest.dto.ObjectDetectionResult;
import com.work.demo.service.DeteccionService;
import com.work.demo.service.dto.AnalisisReturnDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.work.demo.rest.dto.ObjectDetectionContainer;
import com.work.demo.rest.dto.ObjectPruebaDto;
import com.work.demo.service.ObjectDetectionService;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@RestController
public class ObjectDetectionController {
    @Autowired
    private ObjectDetectionService obj;
    @Autowired
    private DeteccionService deteccionService;
    @PostMapping("/detect")
    public List<ObjectDetectionResult> detectObjects(@RequestParam("image") MultipartFile image,@RequestParam("proyectId")Long proyectId) {
        List<ObjectDetectionResult> objectList = new ArrayList<>();
        if(deteccionService.checkToday()){
            throw new RuntimeException("Hoy ya se ha subido foto");
        }
        List<ObjectDetectionResult> results = performObjectDetection(image,proyectId);
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
    @RequestMapping("/detections/image")
    public AnalisisReturnDto getDetectionImage(@RequestParam("image") MultipartFile imageFile,@RequestParam("proyectId")Long proyectId) {
        AnalisisReturnDto response=obj.performAllDetectionsAndReturnImage(imageFile,proyectId);
        return response;
    }

    private List<ObjectDetectionResult> performObjectDetection(MultipartFile image,Long proyectId) {
        // Lista para almacenar los resultados combinados de todas las detecciones
        List<ObjectDetectionResult> combinedResults = new ArrayList<>();

        // Realizar la detección de conos
        List<ObjectDetectionResult> coneDetections = obj.performConeDetection(image,proyectId);
        combinedResults.addAll(coneDetections);

        // Realizar la detección de vehículos
        List<ObjectDetectionResult> vehicleDetections = obj.performVehicleDetection(image,proyectId);
        combinedResults.addAll(vehicleDetections);

        // Realizar la detección de grúas
        List<ObjectDetectionResult> gruasDetections = obj.performGruasDetection(image,proyectId);
        combinedResults.addAll(gruasDetections);

        // Realizar la detección de palets
        List<ObjectDetectionResult> palletDetections = obj.performPalletDetection(image,proyectId);
        combinedResults.addAll(palletDetections);

        // Devolver la lista combinada de todas las detecciones
        return combinedResults;
    }
}

