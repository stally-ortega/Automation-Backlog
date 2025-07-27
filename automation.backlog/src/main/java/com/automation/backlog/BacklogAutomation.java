package com.automation.backlog;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Clase principal encargada de automatizar la revisión de backlog de servicios
 * en un sistema web a través de Selenium WebDriver.
 * 
 * La automatización permite:
 * <ul>
 *   <li>Iniciar sesión en la plataforma.</li>
 *   <li>Consultar incidentes o peticiones según configuración.</li>
 *   <li>Extraer información relevante de actividades.</li>
 *   <li>Exportar los resultados a un archivo Excel.</li>
 * </ul>
 * 
 * Esta clase se apoya en {@link AnalizadorGestiones} para analizar el contenido
 * textual de las actividades.
 */
public class BacklogAutomation {
	private String url;
	private String  srcIframe;
	private String  user;
	private String  passw;
	private String  fechaInicio;
	private String  fechaFin;
    private List<String> servicios;
    private List<String[]> backlog;
    private WebDriver driver;
    private int waitSeconds;
    private String jsonFilePath;
    
    /**
     * Constructor de BacklogAutomation.
     *
     * @param driver WebDriver para interactuar con la interfaz web.
     * @param config Objeto de configuración que contiene URL, credenciales, fechas y servicios.
     * @param jsonPath Ruta del archivo JSON de entrada, utilizada como base para generar la ruta de salida Excel.
     */
    public BacklogAutomation(WebDriver driver, Config config, String jsonPath) {
        this.driver = driver;
        this.url = config.url;
        this.user = config.user;
        this.passw = config.passw;
        this.fechaInicio = config.fechaInicio;
        this.fechaFin = config.fechaFin;
        this.servicios = config.servicios;
        this.srcIframe = "";
        this.waitSeconds = 10;
        this.backlog = new ArrayList<>();
        this.jsonFilePath = jsonPath;
    }
    
    /**
     * Ejecuta la automatización completa:
     * <ul>
     *   <li>Inicia sesión en el sistema.</li>
     *   <li>Revisa incidentes o peticiones según los servicios configurados.</li>
     *   <li>Extrae la información de backlog.</li>
     *   <li>Guarda los resultados en un archivo Excel.</li>
     * </ul>
     *
     * @return Mensaje de resultado indicando éxito o error.
     */
    public String run() {
        
        // Abrimos la pagina
        driver.get(url);

        // Iniciamos sesion
 		driver.findElement(By.id("LoginUsername")).sendKeys(user);
 		driver.findElement(By.id("LoginPassword")).sendKeys(passw);
 		driver.findElement(By.id("loginBtn")).click();
 		
 		if(!servicios.isEmpty() && servicios.get(0).startsWith("IN")) {
 			this.revisarIncidentes();		// Si los servicios a revisar no son IN
 		}
 		else if(!servicios.isEmpty() && servicios.get(0).startsWith("PT")) {
 			this.revisarPeticiones();		// Si los servicios a revisar no son PT
 		}
 		else {
 			System.out.println(new Error("No hay servicios para revisar"));
 			return """

 			        -------------------------------
 			           AUTOMATIZACIÓN FINALIZADA ERROR    
 			           Configuración de servicios incorrecta. Revise el archivo de extensión .json   
 			        -------------------------------

 			        """;
 		}
 		
 		// exportamos los resultados
 		try {
            escribirResultadosEnExcel();
            return """

                    -------------------------------
                       AUTOMATIZACIÓN FINALIZADA OK    
                       Resultados guardados en: '%s'   
                       Nombre del archivo: 'resultado_backlog.xlsx'   
                    -------------------------------

                    """.formatted(jsonFilePath);
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo Excel.");
            e.printStackTrace();
            return """
            	       
                    -------------------------------
                       AUTOMATIZACIÓN FINALIZADA ERROR    
                       No se pudo escribir el archivo Excel. Consulte con el desarrollador   
                    -------------------------------

                    """;
        }
    }
    
    /**
     * Revisa los servicios de tipo "IN" (incidentes) cargando los datos desde la interfaz web
     * y extrayendo información relevante.
     */
    private void revisarIncidentes() {
		this.srcIframe = "/especialistas/cwc/nav.menu?name=navStart&id=ROOT%2FGesti%C3%B3n%20de%20incidentes%2FCola%20de%20incidentes";
		
		// Esperamos a que cargue la pagina	
		// Cuando haya cargado, nos desplazamos a la cola de incidentes
		waitLoadComponentByXpath("//div[@id='ROOT/Gestión de incidentes']").click();
		waitLoadComponentByXpath("//div[@id='ROOT/Gestión de incidentes/Cola de incidentes']//child::a").click();
		
		// Cuando cargue el panel, vamos a las opciones de busqueda
		waitLoadComponentByXpath("//button[@aria-label='Buscar']").click();
		
		
		for (String servicio : servicios) {
			// Localiza el iframe dentro del tab
			WebElement iframe = waitLoadComponentByXpath("//iframe[contains(@src, '" + srcIframe + "')]");
			driver.switchTo().frame(iframe);

			
			// Localiza el input dentro del iframe
		    WebElement input = waitLoadComponentByXpath("//input[@id='X11']");
		    
		    // Ingresa el n° de servicio y le damos enter
		    input.clear();
		    input.sendKeys(servicio);
		    input.click();
		    input.sendKeys(Keys.RETURN);	
			
			try {			
				// Una vez que nos haya cargado la información, vamos a consultar las actividades
				waitLoadComponentByXpath("//a[contains(@class, 'notebookTab') and contains(text(),'Actividades')]").click();
				WebElement activitiesReadOnly = waitLoadComponentByXpath("(//div[contains(@class, 'FormatInputReadonly')]//div[contains(@class,'textareaView')])[2]");
				String activitiesValue = activitiesReadOnly.getText();
				
				// Analizamos y extraemos los datos del back y las afectaciones
				AnalizadorGestiones ag = new AnalizadorGestiones(activitiesValue, fechaInicio, fechaFin);
		        List<String[]> result = ag.revisarBacklog(servicio);
		        backlog.addAll(result);
		        
			} catch (Exception e) {
				// Si el servicio ya está cerrado, pasará al siguiente servicio
				continue;
			} finally {
				// Vuelve al contexto principal después de interactuar
				driver.switchTo().defaultContent();
				
				// Salimos del servicio para poder buscar otro
				waitLoadComponentByXpath("//button[contains(@class, 'x-btn-text') and text()='Cancelar']").click();				
			}		
		}
	}
    
    /**
     * Revisa los servicios de tipo "PT" (peticiones) cargando los datos desde la interfaz web
     * y extrayendo información relevante.
     */
    private void revisarPeticiones() {
		this.srcIframe = "/especialistas/cwc/nav.menu?name=navStart&id=ROOT%2FGesti%C3%B3n%20de%20Peticiones%2FCola%20de%20peticiones";
		
		// Esperamos a que cargue la pagina
		waitLoadComponentByXpath("//div[@id='ROOT/Gestión de Peticiones']").click();
		waitLoadComponentByXpath("//div[@id='ROOT/Gestión de Peticiones/Cola de peticiones']//child::a").click();
		
		// Cuando cargue el panel, vamos a las opciones de busqueda
		waitLoadComponentByXpath("//button[@aria-label='Buscar']").click();
		
		
		for (String servicio : servicios) {
			// Localiza el iframe dentro del tab
			WebElement iframe = waitLoadComponentByXpath("//iframe[contains(@src, '" + srcIframe + "')]");
			driver.switchTo().frame(iframe);
			
			// Localiza el input dentro del iframe
		    WebElement input = waitLoadComponentByXpath("//input[@id='X11']");
		    
		    // Ingresa el n° de servicio y le damos enter
		    input.clear();
		    input.sendKeys(servicio);
		    input.click();
		    input.sendKeys(Keys.ENTER);	
		    
		    try {
		    	// Esperamos a que la información del servicio nos cargue
			    waitLoadComponentByXpath("//a[contains(@class, 'notebookTab') and contains(text(),'Actividades')]").click();
				
				// Una vez que nos haya cargado la información, vamos a consultar las actividades
				WebElement activitiesReadOnly = waitLoadComponentByXpath("(//div[contains(@class, 'FormatInputReadonly')]//div[contains(@class,'textareaView')])[4]");
				String activitiesValue = activitiesReadOnly.getText();
				
				// Analizamos y extraemos los datos del back y las afectaciones
				AnalizadorGestiones ag = new AnalizadorGestiones(activitiesValue, fechaInicio, fechaFin);
		        List<String[]> result = ag.revisarBacklog(servicio);
		        backlog.addAll(result);
			} catch (Exception e) {
				// Si el servicio ya está cerrado, pasará al siguiente servicio
				continue;
			} finally {
				// Vuelve al contexto principal después de interactuar
				driver.switchTo().defaultContent();
				
				// Salimos del servicio para poder buscar otro
				waitLoadComponentByXpath("//button[contains(@class, 'x-btn-text') and text()='Cancelar']").click();
			}		
		}
		
	}
	
    /**
     * Espera hasta que un componente esté disponible y retornable mediante su xpath.
     *
     * @param xpath Expresión XPath del componente a esperar.
     * @return WebElement listo para interactuar.
     */
	private WebElement waitLoadComponentByXpath(String xpath) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(this.waitSeconds));
		return wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
	}
	
	/**
     * Escribe los resultados extraídos en un archivo Excel ubicado junto al archivo JSON de entrada.
     *
     * @throws IOException si ocurre un error al escribir el archivo.
     */
	private void escribirResultadosEnExcel() throws IOException {
        // Obtener la ruta de salida, cambiando la extensión a .xlsx
        File jsonFile = new File(this.jsonFilePath);
        String parentDirectory = jsonFile.getParent();
        String excelOutputFile = Paths.get(parentDirectory, "resultado_backlog.xlsx").toString();
        this.jsonFilePath = parentDirectory;

        // try-with-resources para asegurar que todo se cierre
        try (
			// Crea un nuevo libro de Excel
            Workbook workbook = new XSSFWorkbook();
    		// Crea un flujo de salida
            FileOutputStream fileOut = new FileOutputStream(excelOutputFile)
        ) {
			// Crea una nueva hoja
            Sheet sheet = workbook.createSheet("Backlog");

            // Estilos de la cabecera
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);

            // Crear la fila de la cabecera (Los nombres de columnas)
            String[] headers = {"N° servicio", "Dia afectado", "Analista afectado", "Fecha ult. nota", "Ult. nota"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // Escribir las filas de datos
            int rowNum = 1;
            for (String[] record : this.backlog) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < record.length; i++) {
                    row.createCell(i).setCellValue(record[i]);
                }
            }

            // Ajustar el ancho de las columnas automáticamente
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Escribir el libro de Excel al archivo
            workbook.write(fileOut);
        }
    }

}
