package com.work.demo.service;

import com.work.demo.exceptions.InvalidParameterException;
import com.work.demo.repository.UsuarioRepository;
import com.work.demo.repository.Usuario;
import com.work.demo.service.dto.UsuarioServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Método para convertir de Usuario a UsuarioServiceDto
    private UsuarioServiceDto convertirAUsuarioDto(Usuario usuario) {
        return UsuarioServiceDto.builder()
                .userId(usuario.getUserId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .build();
    }

    // Método para obtener todos los usuarios y convertirlos a DTOs
    public List<UsuarioServiceDto> obtenerTodosUsuarios() {
        try {
            List<Usuario> usuarios = usuarioRepository.findAll();
            return usuarios.stream()
                    .map(this::convertirAUsuarioDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new InvalidParameterException("Error al obtener la lista de usuarios", e);
        }
    }

    // Método para obtener un usuario por su ID y devolver un DTO
    public UsuarioServiceDto obtenerUsuarioPorId(Long userId) {
        try {
            Optional<Usuario> usuario = usuarioRepository.findById(userId);
            return usuario.map(this::convertirAUsuarioDto)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));
        } catch (Exception e) {
            throw new InvalidParameterException("Error al obtener el usuario con ID: " + userId, e);
        }
    }

    // Método para crear un nuevo usuario y devolver un DTO
    public UsuarioServiceDto crearUsuario(UsuarioServiceDto usuarioDto) {
        try {
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setNombre(usuarioDto.getNombre());
            nuevoUsuario.setEmail(usuarioDto.getEmail());

            Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);
            return convertirAUsuarioDto(usuarioGuardado);
        } catch (Exception e) {
            throw new InvalidParameterException("Error al crear el usuario", e);
        }
    }

    // Método para actualizar un usuario existente y devolver un DTO
    public UsuarioServiceDto actualizarUsuario(Long userId, UsuarioServiceDto usuarioActualizadoDto) {
        try {
            Usuario usuarioExistente = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));

            usuarioExistente.setNombre(usuarioActualizadoDto.getNombre());
            usuarioExistente.setEmail(usuarioActualizadoDto.getEmail());

            Usuario usuarioActualizado = usuarioRepository.save(usuarioExistente);
            return convertirAUsuarioDto(usuarioActualizado);
        } catch (Exception e) {
            throw new InvalidParameterException("Error al actualizar el usuario con ID: " + userId, e);
        }
    }

    // Método para eliminar un usuario por su ID
    public void eliminarUsuario(Long userId) {
        try {
            if (usuarioRepository.existsById(userId)) {
                usuarioRepository.deleteById(userId);
            } else {
                throw new InvalidParameterException("No se puede eliminar, usuario no encontrado con ID: " + userId);
            }
        } catch (Exception e) {
            throw new InvalidParameterException("Error al eliminar el usuario con ID: " + userId, e);
        }
    }

    public Long findOrCreateUser (String email, String nombre) {
        try{
            Optional<Usuario> usuario=usuarioRepository.findByEmail(email);
            if(usuario.isPresent()){
                return usuario.get().getUserId();
            }else{
                UsuarioServiceDto u=crearUsuario(new UsuarioServiceDto(null,nombre,email));
                return u.getUserId();
            }
        }catch(Exception e){
            throw e;
        }
    }

    public Usuario findByEmail (String user) {
        return usuarioRepository.findByEmail(user).get();
    }
}
