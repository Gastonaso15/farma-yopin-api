package com.farmayopin.api.service;

import com.farmayopin.api.exception.BadRequestException;
import com.farmayopin.api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    void almacenarImagen_Valida_RetornaRutaRelativaYGuardaArchivo() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-producto.jpg",
                "image/jpeg",
                "contenido simulado de imagen".getBytes()
        );

        String rutaRelativa = fileStorageService.almacenarImagen(file, "producto_1");

        assertNotNull(rutaRelativa);
        assertTrue(rutaRelativa.startsWith(tempDir.toString()));
        assertTrue(rutaRelativa.endsWith(".jpg"));

        // Comprobar que el archivo existe fisicamente
        Path targetPath = tempDir.resolve(Path.of(rutaRelativa).getFileName());
        assertTrue(Files.exists(targetPath));
    }

    @Test
    void almacenarImagen_ExtensionInvalida_LanzaBadRequestException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento.pdf",
                "application/pdf",
                "dummy pdf".getBytes()
        );

        assertThrows(BadRequestException.class, () -> fileStorageService.almacenarImagen(file, "doc"));
    }

    @Test
    void almacenarImagen_ArchivoVacio_LanzaBadRequestException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vacio.png",
                "image/png",
                new byte[0]
        );

        assertThrows(BadRequestException.class, () -> fileStorageService.almacenarImagen(file, "vacio"));
    }

    @Test
    void eliminarImagen_ArchivoExistente_EliminaCorrectamente() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "bytes".getBytes()
        );

        String rutaRelativa = fileStorageService.almacenarImagen(file, "borrar");
        boolean eliminado = fileStorageService.eliminarImagen(rutaRelativa);

        assertTrue(eliminado);
        Path targetPath = tempDir.resolve(Path.of(rutaRelativa).getFileName());
        assertFalse(Files.exists(targetPath));
    }

    @Test
    void cargarRecurso_ArchivoExistente_RetornaResourceValido() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "recurso.webp",
                "image/webp",
                "webp content".getBytes()
        );

        String rutaRelativa = fileStorageService.almacenarImagen(file, "recurso");
        Resource resource = fileStorageService.cargarRecurso(rutaRelativa);

        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void cargarRecurso_Inexistente_LanzaResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class, () -> fileStorageService.cargarRecurso("no_existe.jpg"));
    }
}
