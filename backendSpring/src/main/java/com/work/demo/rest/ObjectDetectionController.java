package com.work.demo.rest;

import com.work.demo.rest.dto.ObjectDetectionResult;
import com.work.demo.service.DeteccionService;
import com.work.demo.service.dto.AnalisisReturnDto;
import com.work.demo.service.dto.ObjetoImagen;
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
        //if(deteccionService.checkToday()){
            //throw new RuntimeException("Hoy ya se ha subido foto");
        //}
        List<ObjectDetectionResult> results = performObjectDetection(image,proyectId).getObjetos();
        return results;
    }
    //Upload file o algo asi
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

    private ObjetoImagen performObjectDetection(MultipartFile image, Long proyectId) {
         return obj.performAllDetections(image,proyectId);
        // Lista para almacenar los resultados combinados de todas las detecciones

    }
}

