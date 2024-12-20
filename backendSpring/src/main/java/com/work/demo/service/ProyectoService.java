package com.work.demo.service;

import com.work.demo.exceptions.InvalidParameterException;
import com.work.demo.repository.*;
import com.work.demo.service.dto.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProyectoService {

    @Autowired
    private ProyectoRepository proyectoRepository;
    @Autowired
    private DeteccionRepository deteccionRepository;
    @Autowired
    private RestriccionRepository restriccionRepository;
    @Autowired
    private FallosRepository fallosRepository;
    @Autowired
    private UsuarioService usuarioService;

    // Método para convertir de Proyecto a ProyectoServiceDto
    private ProyectoServiceDto convertirAProyectoDto(Proyecto proyecto) {
        ProyectoServiceDto dto = new ProyectoServiceDto();
        dto.setIdProyecto(proyecto.getIdProyecto());
        dto.setNombre(proyecto.getNombre());
        dto.setFechaCreacion(proyecto.getFechaCreacion());
        dto.setMinConf(proyecto.getMinConf());
        return dto;
    }

    // Método para obtener todos los proyectos y convertirlos a DTOs
    public List<ProyectoServiceDto> obtenerTodosProyectos() {
        try {
            List<Proyecto> proyectos = proyectoRepository.findAll();
            return proyectos.stream()
                    .map(this::convertirAProyectoDto) // Convierte cada entidad en un DTO
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new InvalidParameterException("Error al obtener la lista de proyectos", e);
        }
    }

    // Método findAll que retorna todos los proyectos
    public List<ProyectoServiceDto> findAll() {
        try {
            List<Proyecto> proyectos = proyectoRepository.findAll();
            return proyectos.stream()
                    .map(this::convertirAProyectoDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new InvalidParameterException("Error al obtener todos los proyectos", e);
        }
    }

    // Método para obtener un proyecto por su ID y devolver un DTO
    public ProyectoServiceDto obtenerProyectoPorId(Long id_proyecto) {
        try {
            Optional<Proyecto> proyecto = proyectoRepository.findById(id_proyecto);
            return proyecto.map(this::convertirAProyectoDto)
                    .orElseThrow(() -> new RuntimeException("Proyecto no encontrado con ID: " + id_proyecto));
        } catch (Exception e) {
            throw new InvalidParameterException("Error al obtener el proyecto con ID: " + id_proyecto, e);
        }
    }
    public Proyecto findById(Long id_proyecto) {
        try {
            Optional<Proyecto> proyecto = proyectoRepository.findById(id_proyecto);
            return proyecto.get();
        } catch (Exception e) {
            throw new InvalidParameterException("Error al obtener el proyecto con ID: " + id_proyecto, e);
        }
    }
    public Proyecto obtenerProyectoPorIdEntidad(Long id_proyecto) {
        try {

            return  proyectoRepository.findById(id_proyecto).get();
        } catch (Exception e) {
            throw new InvalidParameterException("Error al obtener el proyecto con ID: " + id_proyecto, e);
        }
    }

    // Método para crear un nuevo proyecto y devolver un DTO
    @Transactional
    public ProyectoServiceDto crearProyecto(ProyectoServiceDto proyectoDto) {
        try {
            if(proyectoDto==null || proyectoDto.getNombre()==null  || proyectoDto.getNombre().trim().equals("") || proyectoDto.getMinConf()<0 || proyectoDto.getMinConf()>1  ){
                throw new InvalidParameterException("Error al crear el proyecto");
            }
            Proyecto nuevoProyecto = new Proyecto();
            Usuario usuario=usuarioService.findByEmail(proyectoDto.getUser());
            nuevoProyecto.setUsuario(usuario);
            nuevoProyecto.setMinConf(proyectoDto.getMinConf());
            nuevoProyecto.setNombre(proyectoDto.getNombre());
            nuevoProyecto.setFechaCreacion(Date.valueOf(LocalDate.now()));
            nuevoProyecto.setBorrado(false);
            Proyecto proyectoGuardado = proyectoRepository.save(nuevoProyecto);
            return convertirAProyectoDto(proyectoGuardado);
        } catch (Exception e) {
            throw new InvalidParameterException("Error al crear el proyecto", e);
        }
    }

    // Método para actualizar un proyecto existente y devolver un DTO
    public ProyectoServiceDto actualizarProyecto(Long id_proyecto, ProyectoServiceDto proyectoActualizadoDto) {
        try {
            if(proyectoActualizadoDto==null || proyectoActualizadoDto.getNombre()==null  || proyectoActualizadoDto.getNombre().trim().equals("") || proyectoActualizadoDto.getMinConf()<0 || proyectoActualizadoDto.getMinConf()>1  ){
                throw new InvalidParameterException("Error al crear el proyecto");
            }
            Proyecto proyectoExistente = proyectoRepository.findById(id_proyecto)
                    .orElseThrow(() -> new RuntimeException("Proyecto no encontrado con ID: " + id_proyecto));

            proyectoExistente.setNombre(proyectoActualizadoDto.getNombre());
            proyectoExistente.setMinConf(proyectoActualizadoDto.getMinConf());
            Proyecto proyectoActualizado = proyectoRepository.save(proyectoExistente);
            return convertirAProyectoDto(proyectoActualizado);
        } catch (Exception e) {
            throw new InvalidParameterException("Error al actualizar el proyecto con ID: " + id_proyecto, e);
        }
    }

    // Método para eliminar un proyecto por su ID
    public void eliminarProyecto(Long id_proyecto) {
        try {
            if (proyectoRepository.existsById(id_proyecto)) {
                Proyecto proyectoExistente = proyectoRepository.findById(id_proyecto).orElseThrow(() -> new RuntimeException("Proyecto no encontrado con ID: " + id_proyecto));
                proyectoExistente.setBorrado(true);
                proyectoRepository.save(proyectoExistente);
            } else {
                throw new InvalidParameterException("No se puede eliminar, proyecto no encontrado con ID: " + id_proyecto);
            }
        } catch (Exception e) {
            throw new InvalidParameterException("Error al eliminar el proyecto con ID: " + id_proyecto, e);
        }
    }
    @Transactional
    public List<RestriccionServiceDto> findRestrict(Long proyectoId) {
        // Obtenemos todas las restricciones para el proyecto dado
        List<Restriccion> restricciones = restriccionRepository.findRestrictionsByProjectEntity(proyectoId);

// Obtenemos todas las detecciones asociadas a ese proyecto
        List<Deteccion> detecciones = deteccionRepository.findDetectionsByProject(proyectoId);

// Obtenemos la fecha actual
        // Obtener la fecha actual como LocalDate
        LocalDate localDate = LocalDate.now();

// Convertir LocalDate a java.sql.Date
        Date fechaActual = Date.valueOf(localDate);

// Iteramos sobre cada restricción y verificamos solo si fechaHasta es anterior o igual a la fecha actual
        restricciones.forEach(restriccion -> {
            System.out.println(restriccion);
            // Solo evaluamos las restricciones que están dentro del rango de fechas
            if ((restriccion.getFechaHasta().before(fechaActual) || restriccion.getFechaHasta().equals(fechaActual)) && restriccion.getCumplida()==null && !restriccion.getDiaria()) {

                // Filtramos las detecciones que cumplen con el intervalo de fechas y el objeto
                List<Deteccion> deteccionesFiltradas = detecciones.stream()
                        .filter(deteccion ->
                                (deteccion.getFotoId().after(restriccion.getFechaDesde()) || deteccion.getFotoId().equals(restriccion.getFechaDesde())) &&
                                        (deteccion.getFotoId().before(restriccion.getFechaHasta()) || deteccion.getFotoId().equals(restriccion.getFechaHasta())) &&
                                        deteccion.getObjeto().replace(" ", "").toLowerCase().equals(restriccion.getObjeto().replace(" ", "").toLowerCase())
                        ).collect(Collectors.toList());

                // Verificamos la cantidad de detecciones filtradas
                int cantidadDetecciones = deteccionesFiltradas.size();

                // Verificamos si se cumple la cantidad mínima y máxima de detecciones
                if (cantidadDetecciones >= restriccion.getCantidadMin() && cantidadDetecciones <= restriccion.getCantidadMax()) {
                    restriccion.setCumplida(true);  // La restricción se cumple
                } else {
                    restriccion.setCumplida(false);  // La restricción no se cumple

                    // Creamos un nuevo fallo en la base de datos cuando no se cumple la restricción
                    Fallo nuevoFallo = Fallo.builder()
                            .restriccion(restriccion)  // Asociamos la restricción que falló
                            .datos("La restricción no se cumplió: Objeto esperado: " + restriccion.getObjeto() +", se esperaban entre "+restriccion.getCantidadMin()+" y "+
                                    restriccion.getCantidadMax()+" de apariciones y son: " + cantidadDetecciones+", esta comprobacion se ha acabado de incumplir el dia: "+fechaActual)
                            .fecha(fechaActual)
                            .build();

                    // Guardamos el fallo en la base de datos
                    fallosRepository.save(nuevoFallo);
                }

                // Actualizamos el estado de la restricción en la base de datos
                restriccionRepository.save(restriccion);
            }
        });

// Convertimos las restricciones a DTOs para devolverlas al frontend
        return restricciones.stream()
                .map(this::convertirARestriccionDto) // Método que convierte Restriccion a RestriccionServiceDto
                .collect(Collectors.toList());

    }


    private RestriccionServiceDto convertirARestriccionDto(Restriccion restriccion) {
        // Creamos una instancia del DTO
        RestriccionServiceDto dto = new RestriccionServiceDto();

        // Asignamos los valores de la entidad Restriccion al DTO
        dto.setIdRestriccion(restriccion.getIdRestriccion());
        dto.setProyectoId(restriccion.getProyecto().getIdProyecto());
        dto.setObjeto(restriccion.getObjeto());
        dto.setFechaDesde(restriccion.getFechaDesde());
        dto.setFechaHasta(restriccion.getFechaHasta());
        dto.setCantidadMin(restriccion.getCantidadMin());
        dto.setCantidadMax(restriccion.getCantidadMax());
        dto.setCumplida(restriccion.getCumplida());
        dto.setDiaria(restriccion.getDiaria());
        // Retornamos el DTO
        return dto;
    }


    public List<FotoServiceDto> findDetect (Long id) {
        return deteccionRepository.findDetectionsByProjectGroupedByDate(id);
    }

    public List<ProyectoServiceDto> findByEmail (String email,String nombre) {
        try{
            if(email==null || nombre==null  || nombre.trim().equals("") || email.trim().equals("") ){
                throw new InvalidParameterException("Error al crear el proyecto");
            }
            Long usuarioId=usuarioService.findOrCreateUser(email,nombre);
            List<Proyecto> proyectos = proyectoRepository.findByUsuarioUserIdAndBorradoFalse(usuarioId);
            return proyectos.stream()
                    .map(this::convertirAProyectoDto) // Convierte cada entidad en un DTO
                    .collect(Collectors.toList());

        }catch(Exception e){
            throw e;
        }
    }

    public Boolean checkProject (Long projectId, String email) {
        try{
            if(email==null || email.trim().equals("") ){
                throw new InvalidParameterException("Error al comprobar el usuario del proyecto");
            }
            Long usuarioId=usuarioService.findByEmail(email).getUserId();
            Proyecto p=proyectoRepository.findById(projectId).get();
            return p.getUsuario().getUserId()==usuarioId;
        }catch(Exception e){
            throw new InvalidParameterException("email incorrecto",e);
        }
    }
    @Transactional
    public List<FallosServiceDto> findFallosDia (Long code, Date fechaDeteccion) {
        List<Fallo> fallos = fallosRepository.findFallosByProyectoIdAndFechaDeteccion(code, fechaDeteccion);
        return fallos.stream()
                .map(this::convertirAFallosDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<DeteccionServiceDto> findDeteccionesDia (Long code, Date fechaDeteccion) {
        // Buscar las detecciones del proyecto en la fecha indicada
        List<Deteccion> detecciones = deteccionRepository.findByProyectoIdAndFechaDeteccion(code, fechaDeteccion);

        // Convertir las detecciones a DTO
        return detecciones.stream()
                .map(this::convertirADeteccionDto)
                .collect(Collectors.toList());
    }
    private FallosServiceDto convertirAFallosDto(Fallo fallo) {
        return FallosServiceDto.builder()
                .falloId(fallo.getFalloId())
                .restriccionId(fallo.getRestriccion().getIdRestriccion())
                .datos(fallo.getDatos())
                .fecha(fallo.getFecha())
                .build();
    }
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
}

