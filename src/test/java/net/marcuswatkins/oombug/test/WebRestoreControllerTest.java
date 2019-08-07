package net.marcuswatkins.oombug.test;


import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;


import org.junit.Test;
import org.junit.runner.RunWith;
import net.marcuswatkins.oombug.WebRestoreController.WebRestoreConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;


@RunWith( SpringRunner.class )
@SpringBootTest
@AutoConfigureMockMvc
public class WebRestoreControllerTest {

    private static final String CHARSET = "UTF-8";
    @Autowired
    private MockMvc mvc;

    @Configuration
    @TestConfiguration
    public static class ContextConfiguration {
        /**
         * Fake config.
         * 
         * @return fake config
         * @throws IOException never
         */
        @Bean
        @Primary
        public WebRestoreConfig config() throws IOException {
            WebRestoreConfig config = new WebRestoreConfig();
            config.setStoragePath( Files.createTempDirectory( null ).toString() );
            return config;
        }
    }

    @Autowired
    private WebRestoreConfig config;

    @Test
    public void testMissing() throws Exception {
        mvc.perform( MockMvcRequestBuilders.get( "/foo" ) ).andExpect( status().isNotFound() );
    }

    @Test
    public void testGetExists() throws Exception {
        Path file = null;
        try {
            String content = "Test file\ncontent";
            setupFolder();
            file = getStoragePath().resolve( "test1" );
            Files.copy( fakeInputStream( content ), file, StandardCopyOption.REPLACE_EXISTING );
            mvc.perform( MockMvcRequestBuilders.get( "/test1" ).accept( MediaType.ALL_VALUE ) )
                    .andExpect( status().isOk() )
                    .andExpect( content().contentType( MediaType.APPLICATION_OCTET_STREAM ) )
                    .andExpect( content().string( content ) );
        }
        finally {
            if ( file != null ) {
                Files.deleteIfExists( file );
            }
        }
    }

    @Test
    public void testHeadExists() throws Exception {
        Path file = null;
        try {
            String content = "Test file\ncontent";
            setupFolder();
            file = getStoragePath().resolve( "test1" );
            Files.copy( fakeInputStream( content ), file, StandardCopyOption.REPLACE_EXISTING );
            mvc.perform( MockMvcRequestBuilders.head( "/test1" ) )
                    .andExpect( status().isOk() )
                    .andExpect( content().string( "" ) )
                    .andExpect( header().longValue( "Content-length", content.length() ) );
        }
        finally {
            if ( file != null ) {
                Files.deleteIfExists( file );
            }
        }
    }

    @Test
    public void testDel() throws Exception {
        Path file = null;
        try {
            String content = "Test file\ncontent";
            setupFolder();
            file = getStoragePath().resolve( "test1" );
            Files.copy( fakeInputStream( content ), file, StandardCopyOption.REPLACE_EXISTING );
            mvc.perform( MockMvcRequestBuilders.delete( "/test1" ) )
                    .andExpect( status().isNoContent() )
                    .andExpect( content().string( "" ) );
            assert (!Files.exists( file ));
        }
        finally {
            if ( file != null ) {
                Files.deleteIfExists( file );
            }
        }
    }

    @Test
    public void testPut() throws Exception {
        Path file = null;
        try {
            setupFolder();
            file = getStoragePath().resolve( "test1" );
            Files.deleteIfExists( file );
            byte[] content = "Test file\ncontent".getBytes( CHARSET );
            mvc.perform( MockMvcRequestBuilders.put( "/test1" ).content( content ) )
                    .andExpect( status().isCreated() )
                    .andExpect( content().string( "" ) );
            assert (Files.exists( file ));
            assert (Arrays.equals( Files.readAllBytes( file ), content ));
        }
        finally {
            if ( file != null ) {
                Files.deleteIfExists( file );
            }
        }
    }

    @Test
    public void testPutExisting() throws Exception {
        Path file = null;
        try {
            setupFolder();
            file = getStoragePath().resolve( "test1" );
            Files.copy( fakeInputStream( "Existing" ), file, StandardCopyOption.REPLACE_EXISTING );
            byte[] content = "Test file\ncontent".getBytes( CHARSET );
            mvc.perform( MockMvcRequestBuilders.put( "/test1" ).content( content ) )
                    .andExpect( status().isCreated() )
                    .andExpect( content().string( "" ) );
            assert (Files.exists( file ));
            assert (Arrays.equals( Files.readAllBytes( file ), content ));
        }
        finally {
            if ( file != null ) {
                Files.deleteIfExists( file );
            }
        }
    }

    private Path getStoragePath() {
        return Paths.get( config.getStoragePath() );
    }

    private void setupFolder() throws IOException {
        Path folder = getStoragePath();
        if ( !Files.exists( folder ) ) {
            Files.createDirectory( folder );
        }
    }

    private static InputStream fakeInputStream( String content ) 
            throws UnsupportedEncodingException {
        return new ByteArrayInputStream( content.getBytes( CHARSET ) );
    }

}
