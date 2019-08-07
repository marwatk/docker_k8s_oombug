package net.marcuswatkins.oombug;


import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.validation.constraints.NotBlank;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Slf4j
public class WebRestoreController {

    private static final String VALIDATION_REGEX = "^([\\w_-]+)$";

    private Path storagePath;
    
    /**
     * Contructs a WebRestoreController.
     * 
     * @param config Provided by spring
     * @throws IOException when path cannot be created if missing
     */
    @Autowired
    public WebRestoreController( WebRestoreConfig config ) throws IOException {
        if ( config.storagePath == null ) {
            throw new IllegalArgumentException( "Storage path cannot be null" );
        }
        storagePath = Paths.get( config.storagePath ).toAbsolutePath();
        Files.createDirectories( storagePath );
    }
    
    @Data
    @Component
    @Configuration
    @ConfigurationProperties( prefix = "app" )
    public static class WebRestoreConfig {
        private String storagePath;
    }

    /**
     * Handles delete requests.
     * 
     * @param name Image to delete
     * @return Entity for response
     * @throws IOException On io error
     */
    @RequestMapping( value = "/{name}", method = { RequestMethod.DELETE } )
    public ResponseEntity<String> doDelete( 
            @NotBlank @PathVariable( "name" ) String name ) throws IOException {
        Path filePath = resolvePath( name );
        log.info( "DELETE: {}", filePath );
        if ( filePath != null ) {
            Files.delete( filePath );
        }
        return new ResponseEntity<>( "", HttpStatus.NO_CONTENT );
    }

    /**
     * Handles GET and HEAD requests.
     * 
     * @param name Image to check
     * @param request Request
     * @return Response
     * @throws IOException On io error
     */
    @RequestMapping( value = "/{name}", method = { RequestMethod.GET, RequestMethod.HEAD }, 
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE )
    @ResponseBody
    public ResponseEntity<InputStreamResource> doGet( 
            @NotBlank @PathVariable( "name" ) String name, 
            RequestEntity<String> request ) throws IOException {
        Path filePath = resolvePath( name );
        log.info( "GET: [{}]", filePath );
        if ( !Files.exists( filePath ) ) {
            return new ResponseEntity<>( HttpStatus.NOT_FOUND );
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType( MediaType.APPLICATION_OCTET_STREAM );
        headers.setContentLength( Files.size( filePath ) );

        if ( request.getMethod() == HttpMethod.GET ) {
            log.info( "Returning full body" );
            return new ResponseEntity<>( new InputStreamResource( 
                Files.newInputStream( filePath ) ), headers, HttpStatus.OK );
        }
        log.info( "Returning head only" );
        return new ResponseEntity<>( headers, HttpStatus.OK );
    }

    /**
     * Handles PUT requests.
     * 
     * @param name Image to PUT
     * @param request Request
     * @return Response
     * @throws IOException On io error
     */
    @RequestMapping( value = "/{name}", method = { RequestMethod.PUT } )
    public ResponseEntity<String> doPut( @NotBlank @PathVariable( "name" ) String name,
            RequestEntity<InputStreamResource> request ) throws IOException {
        Path filePath = resolvePath( name );
        if ( filePath == null ) {
            log.error( "Invalid put: [{}]", filePath );
            return new ResponseEntity<>( HttpStatus.BAD_REQUEST );
        }
        log.info( "PUT: {}", filePath );
        Path tempFile = null;
        long fileSize = request.getHeaders().getContentLength();
        log.info( "Incoming file size: [{}]", fileSize );
        boolean success = false;
        try {
            tempFile = Files.createTempFile( storagePath, null, null );
            Files.copy( request.getBody().getInputStream(), tempFile, 
                    StandardCopyOption.REPLACE_EXISTING );
            if ( Files.size( tempFile ) == fileSize || fileSize == -1 ) {
                try {
                    log.info( "Moving file from [{}] to [{}] using atomic move",
                            tempFile, filePath );
                    Files.move( tempFile, filePath, StandardCopyOption.ATOMIC_MOVE );
                }
                catch ( AtomicMoveNotSupportedException ex ) {
                    log.info( "Atomic move not supported" );
                    Files.move( tempFile, filePath, StandardCopyOption.REPLACE_EXISTING );
                }
            }
            log.info( "Upload succeeded" );
            success = true;
        }
        catch ( IOException e ) {
            log.error( "Error caught during upload: ", e );
            throw( e );
        }
        finally {
            if ( tempFile != null && Files.exists( tempFile ) ) {
                log.info( "Deleting temp file [{}] from failed operation", tempFile );
                Files.delete( tempFile );
            }
            if ( Files.exists( filePath )
                    && ( ( Files.size( filePath ) != fileSize && fileSize != -1 ) || !success ) ) {
                log.info( "Deleting restore image [{}] from failed operation", filePath );
                Files.delete( filePath );
            }
        }
        return new ResponseEntity<>( HttpStatus.CREATED );
    }

    private Path resolvePath( String name ) {
        if ( !name.matches( VALIDATION_REGEX ) ) {
            log.error( "Invalid request uri [{}]", name );
            return null;
        }
        Path path = storagePath.resolve( name );
        log.info( "Mapped uri [{}] to path [{}]", name, path );
        return path;
    }
}
