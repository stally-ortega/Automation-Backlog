package com.automation.backlog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    public AnalizadorGestiones(String texto, String fechaI, String fechaF) {

        // Definir rango de fechas a analizar
        this.fechaInicio = LocalDate.parse(fechaI, DATE_FORMATTER);
        this.fechaFin = LocalDate.parse(fechaF, DATE_FORMATTER);
        this.texto = texto; // Texto total de las actividades diarias del servicio
        this.fechaPrimeraGestion = LocalDate.parse(fechaI, DATE_FORMATTER);
    }
    
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
    
    private Gestion obtenerUltimaGestion(List<Gestion> gestiones) {
        return gestiones.stream()
                .max(Comparator.comparing(Gestion::getFechaHora))
                .orElseThrow(() -> new RuntimeException("No hay gestiones registradas"));
    }
    
    class Gestion {
        private final LocalDateTime fechaHora;
        private final String autor;
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
