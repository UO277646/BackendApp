package com.work.demo.rest;

import com.work.demo.rest.dto.ObjectDetectionResult;
import com.work.demo.rest.dto.TokenRequestDto;
import com.work.demo.service.DeteccionService;
import com.work.demo.service.PdfGenerator;
import com.work.demo.service.TokenValidatorService;
import com.work.demo.service.dto.AnalisisReturnDto;
import com.work.demo.service.dto.ObjetoImagen;
import net.sf.jasperreports.engine.JRException;
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
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@RestController
public class ObjectDetectionController {
    @Autowired
    private ObjectDetectionService obj;
    @Autowired
    private TokenValidatorService tokenValidatorService;

    @Autowired
    private PdfGenerator pdfGenerator;
    @PostMapping("/detect")
    public ObjetoImagen detectObjects(@RequestParam("image") MultipartFile image,@RequestParam("proyectId")Long proyectId) {
        ObjetoImagen results = performObjectDetection(image,proyectId);
        return results;
    }
    @PostMapping("/camera/detect")
    public ObjetoImagen verifyToken(@RequestParam("image") MultipartFile image,@RequestParam("proyectId")Long proyectId,@RequestParam("token") String tokenRequest) {
        try {
            // Llama al servicio que valida el token y genera el JWT
            boolean jwt = tokenValidatorService.verifyCamera(tokenRequest);
            if (jwt) {
                return  performObjectDetection(image,proyectId);
            } else {
                return null;
            }
        } catch (GeneralSecurityException | IOException e) {
            // Manejar posibles excepciones relacionadas con la verificación del token
            return null;
        }
    }
    //Upload file o algo asi
    @GetMapping("/generate/{id}")
    public byte[] generatePdf(@PathVariable Long id) throws JRException {
        System.out.println(pdfGenerator.generatePdf(id));
        return pdfGenerator.generatePdf(id);
    }

    private ObjetoImagen performObjectDetection(MultipartFile image, Long proyectId) {
         return obj.performAllDetections(image,proyectId);
    }
}

