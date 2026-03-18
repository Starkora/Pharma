package com.pharmasys.service;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.sas.BlobSasPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlobStorageService {

    @Value("${azure.storage.connection-string:}")
    private String connectionString;

    @Value("${azure.storage.container-name:products}")
    private String containerName;

    @Value("${azure.storage.sas-expiry-minutes:240}")
    private long sasExpiryMinutes;

    public String subirImagenProducto(MultipartFile archivo) throws IOException {
        validarImagen(archivo);

        String original = archivo.getOriginalFilename() == null ? "imagen" : archivo.getOriginalFilename();
        String nombreSeguro = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String blobName = "productos/" + UUID.randomUUID() + "_" + nombreSeguro;

        BlobContainerClient containerClient = obtenerContainerClient();
        try (InputStream inputStream = archivo.getInputStream()) {
            containerClient.getBlobClient(blobName).upload(inputStream, archivo.getSize(), true);
            containerClient.getBlobClient(blobName)
                    .setHttpHeaders(new BlobHttpHeaders().setContentType(archivo.getContentType()));
        }

        return containerClient.getBlobClient(blobName).getBlobUrl();
    }

    public void eliminarImagenSiExiste(String imagenUrl) {
        if (imagenUrl == null || imagenUrl.isBlank()) {
            return;
        }

        try {
            BlobContainerClient containerClient = obtenerContainerClient();
            String blobName = extraerBlobName(imagenUrl);
            if (blobName != null && !blobName.isBlank()) {
                containerClient.getBlobClient(blobName).deleteIfExists();
            }
        } catch (Exception e) {
            log.warn("No se pudo eliminar la imagen en Blob Storage");
        }
    }

    public String generarUrlLecturaConSas(String imagenUrl) {
        if (imagenUrl == null || imagenUrl.isBlank()) {
            return imagenUrl;
        }

        try {
            String blobName = extraerBlobName(imagenUrl);
            if (blobName == null || blobName.isBlank()) {
                return imagenUrl;
            }

            BlobContainerClient containerClient = obtenerContainerClient();
            var blobClient = containerClient.getBlobClient(blobName);

            if (!blobClient.exists()) {
                return imagenUrl;
            }

            BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
            OffsetDateTime expiresOn = OffsetDateTime.now().plusMinutes(sasExpiryMinutes);
            BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiresOn, permission);

            return blobClient.getBlobUrl() + "?" + blobClient.generateSas(values);
        } catch (Exception e) {
            log.warn("No se pudo generar SAS para imagen");
            return imagenUrl;
        }
    }

    public String normalizarUrlPersistible(String imagenUrl) {
        if (imagenUrl == null || imagenUrl.isBlank()) {
            return imagenUrl;
        }

        try {
            String blobName = extraerBlobName(imagenUrl);
            if (blobName == null || blobName.isBlank()) {
                return imagenUrl;
            }

            BlobContainerClient containerClient = obtenerContainerClient();
            return containerClient.getBlobClient(blobName).getBlobUrl();
        } catch (Exception e) {
            log.warn("No se pudo normalizar URL de imagen");
            return imagenUrl;
        }
    }

    private void validarImagen(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen valida");
        }

        if (archivo.getSize() > 5L * 1024L * 1024L) {
            throw new IllegalArgumentException("La imagen no debe exceder 5MB");
        }
    }

    private BlobContainerClient obtenerContainerClient() {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalStateException("Falta configurar azure.storage.connection-string");
        }

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.create();
        }

        return containerClient;
    }

    private String extraerBlobName(String imagenUrl) {
        try {
            URI uri = URI.create(imagenUrl);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }

            // Normaliza rutas que llegan codificadas, por ejemplo: productos%2Farchivo.webp
            path = URLDecoder.decode(path, StandardCharsets.UTF_8);

            String containerPrefix = "/" + containerName + "/";
            if (path.startsWith(containerPrefix)) {
                return path.substring(containerPrefix.length());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
