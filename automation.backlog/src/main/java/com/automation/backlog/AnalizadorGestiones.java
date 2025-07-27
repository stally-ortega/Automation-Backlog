package com.automation.backlog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Clase encargada de analizar las actividades de un servicio en función del texto cargado desde el sistema.
 * 
 * Extrae gestiones con sus fechas, autores y notas, identifica días sin actividad (backlog) y
 * genera un resumen estructurado por servicio para su posterior exportación.
 * 
 * <p>Formato esperado para las entradas en el texto:
 * <pre>
 * 12/07/25 14:30:00 Nombre Apellido (Usuario): Nota de gestión...
 * </pre>
 * 
 * <p>Las fechas de inicio y fin definen el rango que será analizado, y las entradas fuera de este
 * intervalo serán ignoradas.
 */
public class AnalizadorGestiones {
	
	private static final Pattern ENTRADA_PATTERN = Pattern.compile(
	    "(\\d{2}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}) [^(]*\\(([^)]+)\\):\\s*(.*?)(?=\\n\\d{2}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}|\\Z)", 
	    Pattern.DOTALL
	);
    
    private final String texto;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private LocalDate fechaPrimeraGestion;
    
    private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss");
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy");
    
    /**
     * Constructor que recibe el texto a analizar y las fechas que delimitan el rango de revisión.
     *
     * @param texto Texto completo con las actividades del servicio.
     * @param fechaI Fecha de inicio del análisis (formato "dd/MM/yy").
     * @param fechaF Fecha de fin del análisis (formato "dd/MM/yy").
     */
    public AnalizadorGestiones(String texto, String fechaI, String fechaF) {

        // Definir rango de fechas a analizar
        this.fechaInicio = LocalDate.parse(fechaI, DATE_FORMATTER);
        this.fechaFin = LocalDate.parse(fechaF, DATE_FORMATTER);
        this.texto = texto; // Texto total de las actividades diarias del servicio
        this.fechaPrimeraGestion = LocalDate.parse(fechaI, DATE_FORMATTER);
    }
    
    /**
     * Revisa las gestiones del texto y devuelve los días con backlog o el último día gestionado si no hubo backlog.
     *
     * @param servicio El identificador del servicio (ej. "IN-123", "PT-456").
     * @return Lista de arreglos de String con los siguientes campos por fila:
     *         [número de servicio, día afectado, autor, fecha de última nota, contenido de la última nota].
     */
    public List<String[]> revisarBacklog(String servicio) {
        List<Gestion> gestiones = parsearGestiones(texto);
        Gestion ultimaGestion = obtenerUltimaGestion(gestiones);
        
        // Obtener días sin gestión y sus autores
        Map<LocalDate, String> diasSinGestionConAutor = encontrarDiasSinGestion(gestiones, fechaInicio, fechaFin);

        // Obtener la última gestión por cada día con gestión
        Map<LocalDate, Gestion> gestionesPorDia = obtenerUltimaGestionPorDia(gestiones);

        List<String[]> resultado = new ArrayList<>();

        // 1. Agregar registros de días sin gestión
        for (Map.Entry<LocalDate, String> entry : diasSinGestionConAutor.entrySet()) {
            resultado.add(new String[]{
            	servicio,
                entry.getKey().format(DATE_FORMATTER),
                entry.getValue(), // Autor de última gestión antes del día sin gestión
                ultimaGestion.getFechaHora().format(DATE_TIME_FORMATTER),
                ultimaGestion.getNota()
            });
        }
        

        // 2. Agregar solo el último día con gestión para los servicios que no tengan días sin gestión
        if(diasSinGestionConAutor.entrySet().isEmpty()) {
        	for (Map.Entry<LocalDate, Gestion> entry : gestionesPorDia.entrySet()) {
                LocalDate dia = entry.getKey();
                Gestion g = entry.getValue();

                // Solo mostramos el último día gestionado, no todos los días
                if (dia.equals(gestionesPorDia.keySet().stream().max(Comparator.naturalOrder()).get())) {
                    resultado.add(new String[]{
                    	servicio,
                        "", // Día afectado vacío
                        g.getAutor(), // Último autor de ese día
                        g.getFechaHora().format(DATE_TIME_FORMATTER),
                        g.getNota()
                    });
                }
            }
        	
        }

        return resultado;
    }
    
    /**
     * Parsea el texto de entrada para extraer gestiones con fecha, autor y nota.
     *
     * @param texto Texto crudo a analizar.
     * @return Lista ordenada cronológicamente de objetos Gestion.
     */
    private List<Gestion> parsearGestiones(String texto) {
        List<Gestion> gestiones = new ArrayList<>();
        Matcher matcher = ENTRADA_PATTERN.matcher(texto);
        
        while (matcher.find()) {
            String fechaHoraStr = matcher.group(1);
            String autor = matcher.group(2).trim();
            String nota = matcher.group(3)
                    .replaceAll("\\n\\s*\\n", " - ")  // Reemplaza dobles saltos con separador
                    .replaceAll("\\n", " ")           // Convierte saltos simples en espacios
                    .trim();
            
            LocalDateTime fechaHora = LocalDateTime.parse(fechaHoraStr, DATE_TIME_FORMATTER);
            gestiones.add(new Gestion(fechaHora, autor, nota));
        }
        
        // Ordenar gestiones por fecha
        List<Gestion> gestionesOrdenadas = gestiones.stream()
            .sorted(Comparator.comparing(Gestion::getFechaHora))
            .collect(Collectors.toList());
        
        // Buscamos la fecha de la primera gestión
        this.fechaPrimeraGestion = gestionesOrdenadas.get(0).getFechaHora().toLocalDate();
        
        return gestionesOrdenadas;
    }
    
    /**
     * Obtiene la última gestión registrada para cada día dentro del rango especificado.
     *
     * @param gestiones Lista de gestiones parseadas.
     * @return Mapa con fecha (día) como clave y la gestión más reciente de ese día como valor.
     */
    private Map<LocalDate, Gestion> obtenerUltimaGestionPorDia(List<Gestion> gestiones) {
        Map<LocalDate, Gestion> ultimaPorDia = new TreeMap<>();

        for (Gestion gestion : gestiones) {
            LocalDate dia = gestion.getFechaHora().toLocalDate();
            if (!dia.isBefore(fechaInicio) && !dia.isAfter(fechaFin)) {
                ultimaPorDia.put(dia, gestion); // Se sobreescribe con la gestión más reciente del día
            }
        }

        return ultimaPorDia;
    }
    
    /**
     * Encuentra días dentro del rango en los que no se realizó ninguna gestión.
     * Para cada día sin gestión, intenta encontrar el autor de la gestión más reciente anterior.
     *
     * @param gestiones Lista completa de gestiones.
     * @param inicio Fecha de inicio del rango.
     * @param fin Fecha de fin del rango.
     * @return Mapa con fecha como clave y el nombre del autor como valor.
     */
    private Map<LocalDate, String> encontrarDiasSinGestion(List<Gestion> gestiones, LocalDate inicio, LocalDate fin) {
        Map<LocalDate, String> diasSinGestionConAutor = new TreeMap<>();

        // Iterar sobre cada día en el rango
        for (LocalDate dia = inicio; !dia.isAfter(fin); dia = dia.plusDays(1)) {
            boolean tieneGestion = false;
            String autor = "Sin autor"; // Si no encontramos autor, lo dejamos como "Sin autor"

            // Verificar si el día tiene una gestión registrada
            for (Gestion gestion : gestiones) {
                if (gestion.getFechaHora().toLocalDate().isEqual(dia)) {
                    tieneGestion = true;
                    autor = gestion.getAutor(); // Si tiene gestión, tomamos el autor
                    break;
                }
            }

            // Si no tiene gestión, marcamos como "sin gestión" y buscamos el autor hacia atrás
            if (!tieneGestion && dia.isAfter(this.fechaPrimeraGestion)) {
                // Iteramos hacia atrás para encontrar el autor de la última gestión anterior
                for (int i = gestiones.size() - 1; i >= 0; i--) {
                    Gestion gestion = gestiones.get(i);
                    if (gestion.getFechaHora().toLocalDate().isBefore(dia)) {
                        autor = gestion.getAutor();
                        break;
                    }
                }
                diasSinGestionConAutor.put(dia, autor);
            }
        }

        return diasSinGestionConAutor;
    }
    
    /**
     * Obtiene la última gestión registrada en la lista.
     *
     * @param gestiones Lista de gestiones.
     * @return La gestión más reciente.
     */
    private Gestion obtenerUltimaGestion(List<Gestion> gestiones) {
        return gestiones.stream()
                .max(Comparator.comparing(Gestion::getFechaHora))
                .orElseThrow(() -> new RuntimeException("No hay gestiones registradas"));
    }
    
    
    /**
     * Clase interna que representa una gestión individual.
     */
    private static class Gestion {
    	
        /**
         * Fecha y hora en que se registró la gestión.
         */
        private final LocalDateTime fechaHora;

        /**
         * Autor o analista que realizó la gestión.
         */
        private final String autor;

        /**
         * Nota de la gestión, ya procesada (sin saltos de línea).
         */
        private final String nota;
        
        public Gestion(LocalDateTime fechaHora, String autor, String nota) {
            this.fechaHora = fechaHora;
            this.autor = autor;
            this.nota = nota;
        }
        
        public LocalDateTime getFechaHora() {
            return fechaHora;
        }
        
        public String getAutor() {
            return autor;
        }
        
        public String getNota() {
            return nota;
        }

		@Override
		public String toString() {
			return "{fechaHora:" + fechaHora + ", autor:" + autor + ", nota:" + nota + "}";
		} 
    }
}
