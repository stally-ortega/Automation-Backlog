package com.automation.backlog;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class ApplicationTests {
	
	String url, user, passw, srcIframe, fechaInicio, fechaFin;
	String[] servicios;
	List<String[]> backlog;
	WebDriver driver;
	int waitSeconds;
	
	@BeforeEach
	void getUp() {
		// Desactiva los logs de Selenium
		Logger seleniumLogger = Logger.getLogger("org.openqa.selenium");
	    seleniumLogger.setLevel(Level.SEVERE);
	    for (Handler handler : seleniumLogger.getHandlers()) {
	        handler.setLevel(Level.SEVERE);
	    }
	    Logger.getLogger("java.lang.Runtime").setLevel(Level.SEVERE);
	    Logger.getLogger("java.lang.ProcessBuilder").setLevel(Level.OFF);
		
		
		// VARIABLES DE LA APLICACION
		waitSeconds = 10;
		backlog = new ArrayList<String[]>();
		backlog.add(new String[] {"N° incidente", "Dia afectado", "Analista afectado", "Fecha ult. nota", "Ult. nota"});
		
		url = "https://mesadeservicio.efecty.com.co/especialistas/index.do?telephonyuser=1&lang=";
		user = "micbelor";	
		passw = "Maik0l1999,,,";
		
		fechaInicio = "17/07/25";
		fechaFin = "24/07/25";
		servicios = new String[]{
				"PT629664","PT629676","PT629697","PT629698","PT629700","PT629702"
		};
		
		// FORMULA PARA EXCEL
		// =TEXTJOIN(",", TRUE, "\"" & A1:A5 & "\"")
		// =UNIRCADENAS(","; VERDADERO; """" & A2:A93 & """")
		
		driver = new ChromeDriver();
		driver.get(url);
	}

	@Test
	void contextLoads() {
		
		System.out.println("-------------------------------");
        System.out.println("      INICIANDO AUTOMATIZACIÓN     ");
        System.out.println("-------------------------------");
		System.out.println("\n\n");
		
		// Iniciamos sesion
		driver.findElement(By.id("LoginUsername")).sendKeys(user);
		driver.findElement(By.id("LoginPassword")).sendKeys(passw);
		driver.findElement(By.id("loginBtn")).click();
		
		if(servicios.length > 0 && servicios[0].startsWith("IN")) {
			this.revisarIncidentes();		// Si los servicios a revisar no son IN
		}
		else if(servicios.length > 0 && servicios[0].startsWith("PT")) {
			this.revisarPeticiones();		// Si los servicios a revisar no son PT
		}
		else {
			System.out.println(new Error("No hay servicios para revisar"));
			return;
		}
		
		// mostrar los resultados
		System.out.println("\n\n");
		
		backlog.forEach(arr -> {
			for(String r : arr) {
	            System.out.print(r + "|");
	        }
			System.out.print("\n");
		});
		
		System.out.println("\n\n");
        System.out.println("-------------------------------");
        System.out.println("     AUTOMATIZACIÓN FINALIZADA OK     ");
        System.out.println("-------------------------------");
		System.out.println("\n\n");
	}
	
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
	
	private WebElement waitLoadComponentByXpath(String xpath) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(this.waitSeconds));
		return wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
	}
}
