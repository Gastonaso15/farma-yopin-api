package com.farmayopin.api.service;

import com.farmayopin.api.dto.direccion.DireccionRequest;
import com.farmayopin.api.dto.direccion.DireccionResponse;
import com.farmayopin.api.exception.ResourceNotFoundException;
import com.farmayopin.api.model.Direccion;
import com.farmayopin.api.model.Usuario;
import com.farmayopin.api.repository.DireccionRepository;
import com.farmayopin.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DireccionService {

    private final DireccionRepository direccionRepository;
    private final UsuarioRepository usuarioRepository;

    public DireccionService(DireccionRepository direccionRepository, UsuarioRepository usuarioRepository) {
        this.direccionRepository = direccionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<DireccionResponse> listarPorUsuario(Long usuarioId) {
        validarUsuarioExiste(usuarioId);
        return direccionRepository.findByUsuarioIdOrderByEsPrincipalDescIdAsc(usuarioId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DireccionResponse crearDireccion(Long usuarioId, DireccionRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));

        boolean debeSerPrincipal = Boolean.TRUE.equals(request.getEsPrincipal())
                || direccionRepository.countByUsuarioId(usuarioId) == 0;

        if (debeSerPrincipal) {
            direccionRepository.resetPrincipalPorUsuario(usuarioId);
        }

        Direccion direccion = Direccion.builder()
                .usuario(usuario)
                .alias(request.getAlias().trim())
                .calle(request.getCalle().trim())
                .numero(request.getNumero().trim())
                .pisoDepto(request.getPisoDepto() != null ? request.getPisoDepto().trim() : null)
                .ciudad(request.getCiudad() != null && !request.getCiudad().trim().isEmpty() ? request.getCiudad().trim() : "Maldonado")
                .departamento(request.getDepartamento() != null && !request.getDepartamento().trim().isEmpty() ? request.getDepartamento().trim() : "Maldonado")
                .codigoPostal(request.getCodigoPostal() != null && !request.getCodigoPostal().trim().isEmpty() ? request.getCodigoPostal().trim() : "20000")
                .notas(request.getNotas() != null ? request.getNotas().trim() : null)
                .esPrincipal(debeSerPrincipal)
                .build();

        Direccion guardada = direccionRepository.save(direccion);
        return mapToResponse(guardada);
    }

    @Transactional
    public DireccionResponse actualizarDireccion(Long usuarioId, Long direccionId, DireccionRequest request) {
        validarUsuarioExiste(usuarioId);
        Direccion direccion = direccionRepository.findByIdAndUsuarioId(direccionId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada con id: " + direccionId));

        if (Boolean.TRUE.equals(request.getEsPrincipal())) {
            direccionRepository.resetPrincipalPorUsuario(usuarioId);
            direccion.setEsPrincipal(true);
        } else if (request.getEsPrincipal() != null) {
            direccion.setEsPrincipal(request.getEsPrincipal());
        }

        direccion.setAlias(request.getAlias().trim());
        direccion.setCalle(request.getCalle().trim());
        direccion.setNumero(request.getNumero().trim());
        direccion.setPisoDepto(request.getPisoDepto() != null ? request.getPisoDepto().trim() : null);
        if (request.getCiudad() != null && !request.getCiudad().trim().isEmpty()) {
            direccion.setCiudad(request.getCiudad().trim());
        }
        if (request.getDepartamento() != null && !request.getDepartamento().trim().isEmpty()) {
            direccion.setDepartamento(request.getDepartamento().trim());
        }
        if (request.getCodigoPostal() != null && !request.getCodigoPostal().trim().isEmpty()) {
            direccion.setCodigoPostal(request.getCodigoPostal().trim());
        }
        direccion.setNotas(request.getNotas() != null ? request.getNotas().trim() : null);

        Direccion actualizada = direccionRepository.save(direccion);
        return mapToResponse(actualizada);
    }

    @Transactional
    public void eliminarDireccion(Long usuarioId, Long direccionId) {
        validarUsuarioExiste(usuarioId);
        Direccion direccion = direccionRepository.findByIdAndUsuarioId(direccionId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada con id: " + direccionId));

        boolean eraPrincipal = Boolean.TRUE.equals(direccion.getEsPrincipal());
        direccionRepository.delete(direccion);

        // Si era la principal y quedan más direcciones, marcar la primera como principal
        if (eraPrincipal) {
            List<Direccion> restantes = direccionRepository.findByUsuarioIdOrderByEsPrincipalDescIdAsc(usuarioId);
            if (!restantes.isEmpty()) {
                Direccion nuevaPrincipal = restantes.get(0);
                nuevaPrincipal.setEsPrincipal(true);
                direccionRepository.save(nuevaPrincipal);
            }
        }
    }

    @Transactional
    public DireccionResponse marcarComoPrincipal(Long usuarioId, Long direccionId) {
        validarUsuarioExiste(usuarioId);
        Direccion direccion = direccionRepository.findByIdAndUsuarioId(direccionId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada con id: " + direccionId));

        direccionRepository.resetPrincipalPorUsuario(usuarioId);
        direccion.setEsPrincipal(true);
        Direccion actualizada = direccionRepository.save(direccion);
        return mapToResponse(actualizada);
    }

    private void validarUsuarioExiste(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId);
        }
    }

    private DireccionResponse mapToResponse(Direccion d) {
        return DireccionResponse.builder()
                .id(d.getId())
                .usuarioId(d.getUsuario() != null ? d.getUsuario().getId() : null)
                .alias(d.getAlias())
                .calle(d.getCalle())
                .numero(d.getNumero())
                .pisoDepto(d.getPisoDepto())
                .ciudad(d.getCiudad())
                .departamento(d.getDepartamento())
                .codigoPostal(d.getCodigoPostal())
                .notas(d.getNotas())
                .esPrincipal(d.getEsPrincipal())
                .fechaCreacion(d.getFechaCreacion())
                .build();
    }
}
