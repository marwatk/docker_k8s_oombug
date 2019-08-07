package net.marcuswatkins.oombug;


import lombok.extern.slf4j.Slf4j;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;


@SpringBootApplication
@Slf4j
public class WebRestoreApplication extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure( SpringApplicationBuilder application ) {
        log.info( "Configuring, inside a servlet container :)" );
        return application.sources( WebRestoreApplication.class );
    }

    public static void main( String[] args ) {
        log.info( "Starting up" );
        SpringApplication.run( WebRestoreApplication.class, args );
    }
}
