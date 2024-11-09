package com.work.demo.service;

import com.work.demo.exceptions.InvalidParameterException;
import com.work.demo.service.dto.DeteccionServiceDto;
import com.work.demo.service.dto.FallosServiceDto;
import net.sf.jasperreports.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PdfGenerator {

    @Autowired
    private ProyectoService p;
    public byte[] generatePdf(Long code, Date fechaDeteccion) {
        try {
            //create a PDF writer instance and pass output stream

            JasperPrint jasperPrint= generateJasperPrintDelegateTemplate(code,fechaDeteccion);
            return JasperExportManager.exportReportToPdf(jasperPrint);
        }
        catch(Exception exp) {
            throw new InvalidParameterException("error generando pdf");
        }

    }
    private JasperPrint generateJasperPrintDelegateTemplate(Long code, Date fechaDeteccion) {
        try{
            List<FallosServiceDto> fallos=p.findFallosDia(code,fechaDeteccion);
            List<DeteccionServiceDto> detecciones=p.findDeteccionesDia(code,fechaDeteccion);
            Map<String,Object> parameters =new HashMap<>();
            parameters.put("logo", ImageIO.read(Objects.requireNonNull(getClass().getResource("/reports/logo.jpg"))));
            parameters.put("uniovi",ImageIO.read(Objects.requireNonNull(getClass().getResource("/reports/uniovi.jpg"))));
            StringBuilder dataBuilder = new StringBuilder();
            for (DeteccionServiceDto deteccion : detecciones) {
                dataBuilder.append("Se detectó un ")
                        .append(deteccion.getObjeto())
                        .append(" con un ")
                        .append(deteccion.getConfidence())
                        .append(" de confianza.\n");
            }
            for (FallosServiceDto fallo : fallos) {
                dataBuilder.append("Ha saltado el siguiente fallo: ")
                        .append(fallo.getDatos())
                        .append(".\n");
            }
            String data = dataBuilder.toString();
            parameters.put("data",data);

            SimpleDateFormat formato = new SimpleDateFormat("dd-MM-yyyy");
            String fecha1Formateada=formato.format(fechaDeteccion);
            parameters.put("fecha",fecha1Formateada);
            //
            parameters.put("tipo","No dispone de tipo");
            InputStream templateStream= getClass().getResourceAsStream("/reports/Resguardo.jrxml");
            JasperReport jasperReport= JasperCompileManager.compileReport(templateStream);
            return JasperFillManager.fillReport(jasperReport,parameters,new JREmptyDataSource());
        }catch(Exception e){
            throw new InvalidParameterException("Error generando el resguardo pdf",e);
        }
    }
}
