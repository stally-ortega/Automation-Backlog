package com.automation.backlog;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
public class Application implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
	@Override
	public void run(String... args) throws Exception {
        // Verificamos que haya argumentos
        if (args.length == 0) {
            System.err.println("Debe proporcionar la ruta al archivo de configuración JSON.");
            System.exit(1);
        }

        // Leemos el archivo JSON 
        ObjectMapper mapper = new ObjectMapper();
        Config config = mapper.readValue(new File(args[0]), Config.class);

        // Silenciamos los logs de Selenium
        Logger seleniumLogger = Logger.getLogger("org.openqa.selenium");
        seleniumLogger.setLevel(Level.SEVERE);

        // Ejecutamos la automatización
        WebDriver driver = new ChromeDriver();
        try {
            BacklogAutomation automation = new BacklogAutomation(driver, config);
            automation.run();
        } finally {
            driver.quit();
        }
    }

}
