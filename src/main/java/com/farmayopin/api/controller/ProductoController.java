package com.farmayopin.api.controller;

import com.farmayopin.api.dto.producto.ProductoCompraHistorialResponse;
import com.farmayopin.api.dto.producto.ProductoRequest;
import com.farmayopin.api.dto.producto.ProductoResponse;
import com.farmayopin.api.dto.producto.UploadImagenResponse;
import com.farmayopin.api.service.FileStorageService;
import com.farmayopin.api.service.ProductoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final FileStorageService fileStorageService;

    public ProductoController(ProductoService productoService, FileStorageService fileStorageService) {
        this.productoService = productoService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listarProductos() {
        List<ProductoResponse> productos = productoService.listarProductos();
        return ResponseEntity.ok(productos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtenerPorId(@PathVariable Long id) {
        ProductoResponse producto = productoService.obtenerPorId(id);
        return ResponseEntity.ok(producto);
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crearProducto(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse creado = productoService.crearProducto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest request) {
        ProductoResponse actualizado = productoService.actualizarProducto(id, request);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        productoService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/compras")
    public ResponseEntity<List<ProductoCompraHistorialResponse>> obtenerHistorialComprasProducto(@PathVariable Long id) {
        List<ProductoCompraHistorialResponse> historial = productoService.obtenerHistorialComprasProducto(id);
        return ResponseEntity.ok(historial);
    }

    /**
     * Sube y asocia directamente una imagen al producto indicado por {id}.
     * Guarda la imagen en la carpeta img/ y persiste la ruta relativa en el campo foto.
     */
    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductoResponse> subirImagenProducto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        ProductoResponse response = productoService.actualizarImagenProducto(id, file);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint general para subir una imagen antes de crear o editar un producto.
     * Retorna la ruta relativa para persistir en la BD y la URL completa de acceso.
     */
    @PostMapping(value = "/imagenes/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadImagenResponse> uploadImagenGeneral(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        String rutaRelativa = fileStorageService.almacenarImagen(file, "upload");

        String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .build()
                .toUriString();

        String urlCompleta = baseUrl + "/" + rutaRelativa;

        UploadImagenResponse response = UploadImagenResponse.builder()
                .rutaRelativa(rutaRelativa)
                .url(urlCompleta)
                .mensaje("Imagen subida exitosamente.")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene el recurso de imagen asociado a un producto por su id.
     */
    @GetMapping("/{id}/imagen")
    public ResponseEntity<Resource> obtenerImagenProducto(
            @PathVariable Long id,
            HttpServletRequest request) {
        ProductoResponse producto = productoService.obtenerPorId(id);
        if (producto.getFoto() == null || producto.getFoto().trim().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = fileStorageService.cargarRecurso(producto.getFoto());

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // Content type default
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * Elimina la imagen asociada al producto {id} y el archivo físico del disco.
     */
    @DeleteMapping("/{id}/imagen")
    public ResponseEntity<ProductoResponse> eliminarImagenProducto(@PathVariable Long id) {
        ProductoResponse response = productoService.eliminarImagenProducto(id);
        return ResponseEntity.ok(response);
    }
}
