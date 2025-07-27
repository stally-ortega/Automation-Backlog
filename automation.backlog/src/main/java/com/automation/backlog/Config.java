package com.automation.backlog;

import java.util.List;

/**
 * Clase de configuración que representa los datos cargados desde el archivo `config.json`.
 * 
 * Esta clase se utiliza para definir los parámetros necesarios para ejecutar la automatización,
 * como la URL del sistema, credenciales de acceso, rango de fechas y la lista de servicios a revisar.
 * 
 * Es deserializada automáticamente mediante Jackson (`ObjectMapper`) desde un archivo JSON.
 *
 * Ejemplo de estructura esperada del archivo:
 * <pre>
 * {
 *   "url": "http://sistema.com",
 *   "user": "miUsuario",
 *   "passw": "miContraseña",
 *   "fechaInicio": "2025-01-01",
 *   "fechaFin": "2025-01-31",
 *   "servicios": ["IN-001", "PT-002"]
 * }
 * </pre>
 */
public class Config {
	 /** URL del sistema al que se conectará el WebDriver. */
    public String url;

    /** Nombre de usuario para autenticarse en el sistema. */
    public String user;

    /** Contraseña correspondiente al usuario. */
    public String passw;

    /** Fecha de inicio del período de revisión (formato esperado: yyyy-MM-dd). */
    public String fechaInicio;

    /** Fecha de fin del período de revisión (formato esperado: yyyy-MM-dd). */
    public String fechaFin;

    /** Lista de identificadores de servicios a revisar (ej: "IN-001", "PT-002"). */
    public List<String> servicios;
}
