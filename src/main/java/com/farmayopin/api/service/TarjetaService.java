package com.farmayopin.api.service;

import com.farmayopin.api.dto.tarjeta.TarjetaRequest;
import com.farmayopin.api.dto.tarjeta.TarjetaResponse;
import com.farmayopin.api.exception.BadRequestException;
import com.farmayopin.api.exception.ResourceNotFoundException;
import com.farmayopin.api.model.Tarjeta;
import com.farmayopin.api.model.TipoTarjeta;
import com.farmayopin.api.model.Usuario;
import com.farmayopin.api.repository.TarjetaRepository;
import com.farmayopin.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TarjetaService {

    private final TarjetaRepository tarjetaRepository;
    private final UsuarioRepository usuarioRepository;

    public TarjetaService(TarjetaRepository tarjetaRepository, UsuarioRepository usuarioRepository) {
        this.tarjetaRepository = tarjetaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<TarjetaResponse> listarPorUsuario(Long usuarioId) {
        validarUsuarioExiste(usuarioId);
        return tarjetaRepository.findByUsuarioIdOrderByEsPrincipalDescIdAsc(usuarioId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TarjetaResponse crearTarjeta(Long usuarioId, TarjetaRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));

        // Obtener últimos 4 dígitos
        String ultimosCuatro = obtenerUltimosCuatro(request);

        // Mapear TipoTarjeta
        TipoTarjeta tipo = TipoTarjeta.CREDITO;
        if (request.getTipo() != null && !request.getTipo().trim().isEmpty()) {
            try {
                tipo = TipoTarjeta.valueOf(request.getTipo().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                tipo = TipoTarjeta.CREDITO;
            }
        }

        boolean debeSerPrincipal = Boolean.TRUE.equals(request.getEsPrincipal())
                || tarjetaRepository.countByUsuarioId(usuarioId) == 0;

        if (debeSerPrincipal) {
            tarjetaRepository.resetPrincipalPorUsuario(usuarioId);
        }

        Tarjeta tarjeta = Tarjeta.builder()
                .usuario(usuario)
                .tipo(tipo)
                .marca(request.getMarca().trim())
                .ultimosCuatro(ultimosCuatro)
                .titular(request.getTitular().trim().toUpperCase())
                .vencimiento(request.getVencimiento().trim())
                .esPrincipal(debeSerPrincipal)
                .build();

        Tarjeta guardada = tarjetaRepository.save(tarjeta);
        return mapToResponse(guardada);
    }

    @Transactional
    public void eliminarTarjeta(Long usuarioId, Long tarjetaId) {
        validarUsuarioExiste(usuarioId);
        Tarjeta tarjeta = tarjetaRepository.findByIdAndUsuarioId(tarjetaId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada con id: " + tarjetaId));

        boolean eraPrincipal = Boolean.TRUE.equals(tarjeta.getEsPrincipal());
        tarjetaRepository.delete(tarjeta);

        // Si era principal y quedan tarjetas, marcar la primera como principal
        if (eraPrincipal) {
            List<Tarjeta> restantes = tarjetaRepository.findByUsuarioIdOrderByEsPrincipalDescIdAsc(usuarioId);
            if (!restantes.isEmpty()) {
                Tarjeta nuevaPrincipal = restantes.get(0);
                nuevaPrincipal.setEsPrincipal(true);
                tarjetaRepository.save(nuevaPrincipal);
            }
        }
    }

    @Transactional
    public TarjetaResponse marcarComoPrincipal(Long usuarioId, Long tarjetaId) {
        validarUsuarioExiste(usuarioId);
        Tarjeta tarjeta = tarjetaRepository.findByIdAndUsuarioId(tarjetaId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada con id: " + tarjetaId));

        tarjetaRepository.resetPrincipalPorUsuario(usuarioId);
        tarjeta.setEsPrincipal(true);
        Tarjeta actualizada = tarjetaRepository.save(tarjeta);
        return mapToResponse(actualizada);
    }

    private String obtenerUltimosCuatro(TarjetaRequest request) {
        if (request.getUltimosCuatro() != null && request.getUltimosCuatro().trim().length() >= 4) {
            String digits = request.getUltimosCuatro().replaceAll("\\D", "");
            if (digits.length() >= 4) {
                return digits.substring(digits.length() - 4);
            }
        }
        if (request.getNumeroTarjeta() != null) {
            String digits = request.getNumeroTarjeta().replaceAll("\\D", "");
            if (digits.length() >= 4) {
                return digits.substring(digits.length() - 4);
            }
        }
        throw new BadRequestException("Debe ingresar un número de tarjeta válido o sus últimos 4 dígitos");
    }

    private void validarUsuarioExiste(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId);
        }
    }

    private TarjetaResponse mapToResponse(Tarjeta t) {
        return TarjetaResponse.builder()
                .id(t.getId())
                .usuarioId(t.getUsuario() != null ? t.getUsuario().getId() : null)
                .tipo(t.getTipo().name())
                .marca(t.getMarca())
                .ultimosCuatro(t.getUltimosCuatro())
                .titular(t.getTitular())
                .vencimiento(t.getVencimiento())
                .esPrincipal(t.getEsPrincipal())
                .fechaCreacion(t.getFechaCreacion())
                .build();
    }
}
