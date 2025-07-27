package com.automation.backlog;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bonigarcia.wdm.WebDriverManager;

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
        

        // Ejecutamos la automatización
        WebDriver driver = new ChromeDriver();
        try {
        	// Leemos el archivo JSON 
        	System.out.println("Paso 0: Leyendo archivo JSON...");
            ObjectMapper mapper = new ObjectMapper();
            Config config = mapper.readValue(new File(args[0]), Config.class);
            System.out.println("Paso 0: ¡Completado!");

            // Silenciamos los logs de Selenium
            Logger seleniumLogger = Logger.getLogger("org.openqa.selenium");
            seleniumLogger.setLevel(Level.SEVERE);
            
            // WebDriverManager configura el driver de Chrome automáticamente ✅
            System.out.println("Paso 1: Configurando WebDriverManager...");
            WebDriverManager.chromedriver().setup();
            System.out.println("Paso 1: ¡Completado!");

            // Configura las opciones de Chrome para que sea visible
            System.out.println("Paso 2: Configurando Opciones de Chrome...");
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--start-maximized");
            System.out.println("Paso 2: ¡Completado!");
            System.out.println("Paso 3: Iniciando el navegador Chrome...");
            driver = new ChromeDriver(options);
            System.out.println("Paso 3: ¡Navegador iniciado con éxito!");

            // Inicia el WebDriver con las opciones configuradas
            System.out.println("Paso 4: Ejecutando la automatización principal...");
            BacklogAutomation automation = new BacklogAutomation(driver, config, args[0]);
            automation.run();
            System.out.println("Paso 4: ¡Automatización completada!");
        } catch (Throwable t) {
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.err.println("!!!      SE PRODUJO UN ERROR INESPERADO     !!!");
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            t.printStackTrace();

        } finally {
        	if (driver != null) {
                System.out.println("Cerrando el navegador...");
                driver.quit();
            }
        }
    }

}
