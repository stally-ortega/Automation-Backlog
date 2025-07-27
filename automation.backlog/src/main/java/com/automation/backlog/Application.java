package com.automation.backlog;

import java.util.Scanner;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.InputStream;

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
		
		Scanner scanner = new Scanner(System.in);

        // Bucle infinito para mostrar el menú hasta que el usuario elija salir
        while (true) {
            System.out.println("\n======================================");
            System.out.println("  AUTOMATIZACIÓN DE REVISIÓN DE BACKLOG ");
            System.out.println("======================================");
            System.out.println("Por favor, elige una opción:");
            System.out.println("  1. Revisar backlog (usar un archivo config.json existente)");
            System.out.println("  2. Descargar plantilla de configuración (config.json)");
            System.out.println("  2. Descargar el manual del usuario");
            System.out.println("  3. Salir");
            System.out.print("Tu elección: ");

            String opcion = scanner.nextLine();
            String templatePath = "";

            switch (opcion) {
                case "1":
                    System.out.println("\nPor favor, arrastra o escribe la ruta completa de tu archivo 'config.json'");
                    System.out.print("Responda aquí: ");
                    templatePath = scanner.nextLine().trim();
                    executeAutomation(templatePath);
                    break;
                case "2":
                    System.out.println("\nEscribe la ruta completa donde quieres guardar la plantilla (ej: C:\\Users\\TuUsuario\\Desktop)");
                    System.out.print("Responda aquí: ");
                    templatePath = scanner.nextLine().trim();
                    downloadFile(templatePath, "/config.json");
                    break;
                case "3":
                    System.out.println("\nEscribe la ruta completa donde quieres guardar el manual (ej: C:\\Users\\TuUsuario\\Desktop): ");
                    System.out.print("Responda aquí: ");
                    templatePath = scanner.nextLine().trim();
                    downloadFile(templatePath, "/user_manual.pdf");
                    break;
                case "4":
                    System.out.println("\nSaliendo de la aplicación...");
                    scanner.close();
                    System.exit(0);
                    return;
                default:
                    System.out.println("Opción no válida. Por favor, intenta de nuevo.");
            }
        }
    }
	
	/**
     * Contiene la lógica original para ejecutar la automatización de Selenium.
     * @param jsonPath La ruta al archivo de configuración del usuario.
     */
    private void executeAutomation(String jsonPath) {
    	// Verificamos que haya argumentos
        if (jsonPath.isBlank() || jsonPath.isEmpty()) {
            System.err.println("Debe proporcionar la ruta al archivo de configuración JSON.");
            System.exit(1);
        }
        
        System.out.println("-------------------------------");
        System.out.println("      INICIANDO AUTOMATIZACIÓN     ");
        System.out.println("-------------------------------\n\n");
        

        // Ejecutamos la automatización
        WebDriver driver = new ChromeDriver();
        String result = """
        	    
        	    -------------------------------
        	       AUTOMATIZACIÓN FINALIZADA ERROR    
        	    -------------------------------
        	    
        	    """;
        
        try {
        	// Leemos el archivo JSON 
        	System.out.println("Paso 1: Leyendo archivo JSON...");
            ObjectMapper mapper = new ObjectMapper();
            Config config = mapper.readValue(new File(jsonPath), Config.class);
            System.out.println("Paso 1: ¡Completado!\n");
            
            // WebDriverManager configura el driver de Chrome automáticamente
            System.out.println("Paso 2: Configurando WebDriverManager...");
            WebDriverManager.chromedriver().setup();
            System.out.println("Paso 2: ¡Completado!\n");

            // Configura las opciones de Chrome para que sea visible
            System.out.println("Paso 3: Configurando Opciones de Chrome...");
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--start-maximized");
            System.out.println("Paso 3: ¡Completado!\n");
            
            System.out.println("Paso 4: Iniciando el navegador Chrome...");
            driver = new ChromeDriver(options);
            System.out.println("Paso 4: ¡Navegador iniciado con éxito!\n");

            // Inicia el WebDriver con las opciones configuradas
            System.out.println("Paso 5: Ejecutando la automatización principal...");
            BacklogAutomation automation = new BacklogAutomation(driver, config, jsonPath);
            result = automation.run();
            System.out.println("Paso 5: ¡Automatización completada!");
            
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
        	
        	// Mostramos mensaje de respuesta final
            System.out.println(result);
        }
    }
    
    
    /**
     * Lee la plantilla desde dentro del JAR y la guarda en la ruta especificada por el usuario.
     * @param rutaDestino La ruta completa donde se guardará el archivo.
     */
    private void downloadFile(String downloadPath, String filename) {
        // Verificamos que la ruta tenga la terminación correcta
        if (downloadPath.endsWith("\\")) {
        	downloadPath = downloadPath.substring(0, downloadPath.length() - 1);
        }
        downloadPath = Paths.get(downloadPath, filename).toString();
        
     // Traemos los datos del archivo ubicado en la ruta src/main/resources
        try (InputStream inputStream = Application.class.getResourceAsStream(filename);
             OutputStream outputStream = new FileOutputStream(downloadPath)) {

            if (inputStream == null) {
                System.err.println("Error: No se pudo encontrar la plantilla '" + filename);
                return;
            }

            // Copia el contenido del archivo de la plantilla al nuevo archivo
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            System.out.println("\n✅ Plantilla descargada con éxito en: " + downloadPath);

        } catch (IOException e) {
            System.err.println("\nError al guardar la plantilla. Asegúrate de que la ruta sea válida.");
            e.printStackTrace();
        }
    }

}
