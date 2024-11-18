package com.work.demo.service;

import com.work.demo.exceptions.InvalidParameterException;
import com.work.demo.repository.Deteccion;
import com.work.demo.repository.DeteccionRepository;
import com.work.demo.repository.Proyecto;
import com.work.demo.service.dto.DeteccionServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.work.demo.service.ObjectDetectionService.encodeImageToBase64;

@Service
public class DeteccionService {
    @Autowired
    private ProyectoService proyectoService;
    @Autowired
    private DeteccionRepository deteccionRepository;

    // Método para convertir de Deteccion a DeteccionServiceDto
    private DeteccionServiceDto convertirADeteccionDto(Deteccion deteccion) {
        return DeteccionServiceDto.builder()
                .deteccionId(deteccion.getDeteccionId())
                .proyectoId(deteccion.getProyecto().getIdProyecto())
                .fotoId(deteccion.getFotoId())
                .objeto(deteccion.getObjeto())
                .x(deteccion.getX())
                .y(deteccion.getY())
                .weight(deteccion.getWeight())
                .height(deteccion.getHeight())
                .confidence(deteccion.getConfidence())
                .build();
    }

    // Método para obtener todas las detecciones y convertirlas a DTOs
    public List<DeteccionServiceDto> obtenerTodasDetecciones() {
        try {
            List<Deteccion> detecciones = deteccionRepository.findAll();
            return detecciones.stream()
                    .map(this::convertirADeteccionDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new InvalidParameterException("Error al obtener la lista de detecciones", e);
        }
    }

    // Método findAll que retorna todas las detecciones
    public List<DeteccionServiceDto> findAll() {
        try {
            List<Deteccion> detecciones = deteccionRepository.findAll();
            return detecciones.stream()
                    .map(this::convertirADeteccionDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new InvalidParameterException("Error al obtener todas las detecciones", e);
        }
    }

    // Método para obtener una detección por su ID y devolver un DTO
    public DeteccionServiceDto obtenerDeteccionPorId(Long deteccionId) {
        try {
            Optional<Deteccion> deteccion = deteccionRepository.findById(deteccionId);
            return deteccion.map(this::convertirADeteccionDto)
                    .orElseThrow(() -> new RuntimeException("Detección no encontrada con ID: " + deteccionId));
        } catch (Exception e) {
            throw new InvalidParameterException("Error al obtener la detección con ID: " + deteccionId, e);
        }
    }

    // Método para crear una nueva detección y devolver un DTO
    public DeteccionServiceDto crearDeteccion(DeteccionServiceDto deteccionDto) {
        try {
            Proyecto p=proyectoService.obtenerProyectoPorIdEntidad(deteccionDto.getProyectoId());
            Deteccion nuevaDeteccion = Deteccion.builder()
                    .proyecto(p)
                    .fotoId(new Date(System.currentTimeMillis()))
                    .objeto(deteccionDto.getObjeto())
                    .x(deteccionDto.getX())
                    .y(deteccionDto.getY())
                    .weight(deteccionDto.getWeight())
                    .height(deteccionDto.getHeight())
                    .confidence(deteccionDto.getConfidence())
                    .build();

            Deteccion deteccionGuardada = deteccionRepository.save(nuevaDeteccion);
            return convertirADeteccionDto(deteccionGuardada);
        } catch (Exception e) {
            throw new InvalidParameterException("Error al crear la detección", e);
        }
    }

    // Método para actualizar una detección existente y devolver un DTO
    public DeteccionServiceDto actualizarDeteccion(Long deteccionId, DeteccionServiceDto deteccionActualizadaDto) {
        try {
            Proyecto p=proyectoService.obtenerProyectoPorIdEntidad(deteccionActualizadaDto.getProyectoId());
            Deteccion deteccionExistente = deteccionRepository.findById(deteccionId)
                    .orElseThrow(() -> new RuntimeException("Detección no encontrada con ID: " + deteccionId));

            deteccionExistente.setProyecto(p);
            deteccionExistente.setFotoId(deteccionActualizadaDto.getFotoId());
            deteccionExistente.setObjeto(deteccionActualizadaDto.getObjeto());
            deteccionExistente.setX(deteccionActualizadaDto.getX());
            deteccionExistente.setY(deteccionActualizadaDto.getY());
            deteccionExistente.setHeight(deteccionActualizadaDto.getHeight());
            deteccionExistente.setWeight(deteccionActualizadaDto.getWeight());
            deteccionExistente.setConfidence(deteccionActualizadaDto.getConfidence());

            Deteccion deteccionActualizada = deteccionRepository.save(deteccionExistente);
            return convertirADeteccionDto(deteccionActualizada);
        } catch (Exception e) {
            throw new InvalidParameterException("Error al actualizar la detección con ID: " + deteccionId, e);
        }
    }

    // Método para eliminar una detección por su ID
    public void eliminarDeteccion(Long deteccionId) {
        try {
            if (deteccionRepository.existsById(deteccionId)) {
                deteccionRepository.deleteById(deteccionId);
            } else {
                throw new InvalidParameterException("No se puede eliminar, detección no encontrada con ID: " + deteccionId);
            }
        } catch (Exception e) {
            throw new InvalidParameterException("Error al eliminar la detección con ID: " + deteccionId, e);
        }
    }

    public boolean checkToday () {
        try {
            // Obtener la fecha actual
            LocalDate today = LocalDate.now();

            // Obtener todas las detecciones
            List<Deteccion> detecciones = deteccionRepository.findAll();

            // Comprobar si alguna detección tiene un 'fotoId' con la fecha de hoy
            return detecciones.stream().anyMatch(deteccion -> {
                // Convertir el campo 'fotoId' de Date a LocalDate
                LocalDate fotoDate = deteccion.getFotoId().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                return fotoDate.equals(today);
            });
        } catch (Exception e) {
            throw new InvalidParameterException("Error al comprobar las detecciones de hoy", e);
        }
    }

    public List<DeteccionServiceDto> findByFotoId (Date fotoId) {
        // Llama al repositorio para buscar las detecciones por fotoId
        if(fotoId==null){
            throw new InvalidParameterException("Foto null");
        }
        return deteccionRepository.findByFotoId(fotoId);
    }

    public List<DeteccionServiceDto> findByProyectoAndDia (Long proyecto, Date dia) {
        if(proyecto==null || dia==null){
            throw new InvalidParameterException("Proyecto o dia vacio");
        }
        List<Deteccion> detecciones = deteccionRepository.findByProyectoIdAndFotoId(proyecto, dia);

        // Convertimos la entidad Deteccion a DeteccionServiceDto
        return detecciones.stream()
                .map(this::convertirADeteccionDto)
                .collect(Collectors.toList());
    }

    public int findLastId () {
        return deteccionRepository.findLastId();
    }

    public String getImagenDia(Long proyecto, Date dia) {
        if (proyecto == null || dia == null) {
            throw new InvalidParameterException("Proyecto o día vacíos");
        }

        // Buscar las detecciones del proyecto en la fecha especificada
        List<Deteccion> detecciones = deteccionRepository.findByProyectoIdAndFotoId(proyecto, dia);

        if (detecciones.isEmpty()) {
            throw new IllegalArgumentException("No hay detecciones para el proyecto y fecha especificados");
        }

        try {
            // Construir la ruta de la imagen con base en el proyecto y la fecha
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(dia);
            String imagePath = "C:\\Users\\user\\Desktop\\" + proyecto + "_" + formattedDate + ".jpg";

            File inputFile = new File(imagePath);

            if (!inputFile.exists()) {
                throw new FileNotFoundException("La imagen asociada no existe en la ruta: " + imagePath);
            }

            // Cargar la imagen original
            BufferedImage baseImage = ImageIO.read(inputFile);

            // Crear una copia de la imagen para dibujar
            BufferedImage outputImage = new BufferedImage(
                    baseImage.getWidth(),
                    baseImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D g2d = outputImage.createGraphics();
            g2d.drawImage(baseImage, 0, 0, null);

// Configurar el estilo de dibujo
            g2d.setColor(Color.RED); // Color de los rectángulos
            g2d.setStroke(new BasicStroke(2)); // Grosor del borde

// Configurar el estilo del texto
            Font font = new Font("Arial", Font.BOLD, 14); // Cambia el tamaño según sea necesario
            g2d.setFont(font);
            FontMetrics metrics = g2d.getFontMetrics();

            for (Deteccion deteccion : detecciones) {
                // Calcular las coordenadas del rectángulo desde el centro y dimensiones
                int x1 = Math.round((float) deteccion.getX()); // Esquina superior izquierda, X
                int y1 = Math.round((float) deteccion.getY()); // Esquina superior izquierda, Y
                int x2 = Math.round((float) deteccion.getWeight()); // Esquina inferior derecha, X
                int y2 = Math.round((float) deteccion.getHeight()); // Esquina inferior derecha, Y

                // Calcular el ancho y alto del rectángulo
                int rectWidth = x2 - x1;
                int rectHeight = y2 - y1;

                // Dibujar el rectángulo
                g2d.drawRect(x1, y1, rectWidth, rectHeight);

                // Crear el texto con la confianza y el objeto
                String label = String.format("%s (%.2f)", deteccion.getObjeto(), deteccion.getConfidence());

                // Calcular el tamaño del texto
                int textWidth = metrics.stringWidth(label);
                int textHeight = metrics.getHeight();

                // Dibujar un fondo rojo opaco para el texto
                g2d.setColor(new Color(255, 0, 0, 200)); // Rojo con transparencia
                g2d.fillRect(x1, y1 + rectHeight - textHeight, textWidth, textHeight);

                // Dibujar el texto en blanco
                g2d.setColor(Color.BLACK);
                g2d.drawString(label, x1, y1 + rectHeight - 5);
            }

            g2d.dispose();

            // Guardar la imagen generada con un nuevo nombre para no sobreescribir
            String outputImagePath = "C:\\Users\\user\\Desktop\\" + proyecto + "_" + formattedDate + "_result.jpg";
            File outputFile = new File(outputImagePath);
            ImageIO.write(outputImage, "jpg", outputFile);

            return encodeImageToBase64(outputImage);

        } catch (IOException e) {
            throw new RuntimeException("Error al procesar la imagen", e);
        }
    }

}
