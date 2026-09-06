package com.farmayopin.api.service;

import com.farmayopin.api.exception.BadRequestException;
import com.farmayopin.api.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadPath;
    private final String uploadDir;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp"
    );

    public FileStorageService(@Value("${app.upload.dir:img}") String uploadDir) {
        this.uploadDir = uploadDir;
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de subida de imagenes: " + this.uploadPath, e);
        }
    }

    /**
     * Guarda un archivo Multipart en la carpeta de subida y retorna su ruta relativa (ej. "img/prod_1_uuid.jpg").
     */
    public String almacenarImagen(MultipartFile archivo, String prefijo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("El archivo de imagen no puede estar vacio.");
        }

        String originalFilename = archivo.getOriginalFilename();
        if (originalFilename == null) {
            throw new BadRequestException("Nombre de archivo invalido.");
        }

        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException("Formato de imagen no permitido. Formatos validos: JPG, JPEG, PNG, WEBP.");
        }

        String contentType = archivo.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Tipo de contenido no permitido: " + contentType);
        }

        String cleanPrefix = (prefijo != null && !prefijo.trim().isEmpty())
                ? prefijo.replaceAll("[^a-zA-Z0-9_\\-]", "") + "_"
                : "prod_";

        String nuevoNombre = cleanPrefix + UUID.randomUUID().toString().substring(0, 8) + "_" + System.currentTimeMillis() + "." + extension.toLowerCase();

        try {
            Path targetLocation = this.uploadPath.resolve(nuevoNombre);
            Files.copy(archivo.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Retornar la ruta relativa con respecto a la raíz del proyecto (ej: "img/nuevoNombre.jpg")
            return this.uploadDir + "/" + nuevoNombre;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la imagen en disco: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina el archivo fisico del disco a partir de su ruta relativa o nombre de archivo.
     */
    public boolean eliminarImagen(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.trim().isEmpty()) {
            return false;
        }

        try {
            String filename = Paths.get(rutaRelativa).getFileName().toString();
            Path filePath = this.uploadPath.resolve(filename).normalize();

            if (Files.exists(filePath) && filePath.startsWith(this.uploadPath)) {
                return Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            // Se registra el fallo pero no se interrumpe la transaccion
            return false;
        }
        return false;
    }

    /**
     * Carga el archivo como Resource de Spring para lectura o streaming.
     */
    public Resource cargarRecurso(String nombreArchivoOPath) {
        if (nombreArchivoOPath == null || nombreArchivoOPath.trim().isEmpty()) {
            throw new ResourceNotFoundException("Ruta de archivo no especificada.");
        }

        try {
            String filename = Paths.get(nombreArchivoOPath).getFileName().toString();
            Path filePath = this.uploadPath.resolve(filename).normalize();

            if (!filePath.startsWith(this.uploadPath)) {
                throw new BadRequestException("Ruta de archivo invalida.");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Imagen no encontrada: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Error al acceder a la imagen: " + e.getMessage());
        }
    }

    public Path getUploadPath() {
        return uploadPath;
    }

    public String getUploadDir() {
        return uploadDir;
    }
}
