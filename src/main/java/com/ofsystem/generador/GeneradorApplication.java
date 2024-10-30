package com.ofsystem.generador;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

@SpringBootApplication
public class GeneradorApplication implements CommandLineRunner {

	private String basePackage; // Agregado para almacenar el paquete base
	private String pkType; // Agregado para almacenar el tipo de clave primaria

	public static void main(String[] args) {
		SpringApplication.run(GeneradorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Ingresa tipo de PK (Integer, Long, String): ");
		pkType = scanner.nextLine();

		System.out.println("Por favor, ingresa la ruta de la entidad: ");
		String entityPath = scanner.nextLine();

		basePackage = getBasePackage(entityPath); // Inicializar basePackage

		generateClasses(entityPath);
	}

	private void generateClasses(String entityPath) {
		try {
			String className = getClassName(entityPath);
			String classNameLower = className.toLowerCase();

			// Generar las clases usando las plantillas
			createClassFromTemplate("src/main/resources/templates/controller-template.txt",
					basePackage + "/Controller/" + className + "Controller.java",
					className, classNameLower);

			createClassFromTemplate("src/main/resources/templates/dao-template.txt",
					basePackage + "/Dao/" + className + "Dao.java",
					className, classNameLower);

			createClassFromTemplate("src/main/resources/templates/repo-template.txt",
					basePackage + "/Repo/I" + className + "Repo.java",
					className, classNameLower);

			createClassFromTemplate("src/main/resources/templates/service-template.txt",
					basePackage + "/Service/I" + className + "Service.java",
					className, classNameLower);

			// Mostrar la ruta donde se crearon los archivos
			System.out.println("Clases generadas correctamente para " + className);
			System.out.println("Rutas de los archivos generados:");
			System.out.println(basePackage + "/Controller/" + className + "Controller.java");
			System.out.println(basePackage + "/Dao/" + className + "Dao.java");
			System.out.println(basePackage + "/Repo/I" + className + "Repo.java");
			System.out.println(basePackage + "/Service/I" + className + "Service.java");

		} catch (Exception e) {
			System.err.println("Error al generar clases: " + e.getMessage());
		}
	}

	private String getClassName(String entityPath) {
		return Paths.get(entityPath).getFileName().toString().replace(".java", "");
	}

	private String getBasePackage(String entityPath) {
		String normalizedPath = entityPath.replace("\\", "/");
		int startIndex = normalizedPath.indexOf("src/main/java/");

		if (startIndex == -1) {
			throw new IllegalArgumentException("La ruta de la entidad no contiene 'src/main/java/'");
		}

		String packagePath = normalizedPath.substring(startIndex + "src/main/java/".length()).replace(".java", "");

		// Convertir la ruta del paquete a una lista de segmentos
		String[] segments = packagePath.split("/");

		// Retornar los primeros tres segmentos como paquete base
		if (segments.length >= 3) {
			return segments[0] + "." + segments[1] + "." + segments[2];
		} else if (segments.length == 2) {
			return segments[0] + "." + segments[1];
		}

		// Si no hay suficientes segmentos, lanzar una excepción o manejarlo según tu lógica
		throw new IllegalArgumentException("La ruta de la entidad no contiene un paquete válido.");
	}

	private void createClassFromTemplate(String templatePath, String outputPath, String className, String classNameLower) throws IOException {
		// Leer la plantilla y reemplazar los marcadores de posición
		String template = new String(Files.readAllBytes(Paths.get(templatePath)));
		String content = template
				.replace("{{className}}", className)
				.replace("{{classNameLower}}", classNameLower)
				.replace("{classNameLower}", classNameLower)
				.replace("{{pkType}}", pkType)
				.replace("{{basePackage}}", basePackage);

		// A outputPath elimínale el .java
		outputPath = outputPath.replace(".java", "");

		// Crear directorios si no existen
		String outputDir = "src/main/java/" + outputPath.replace(".", "/"); // Mantenemos esto para la ruta

		// Separa el nombre del archivo y su ruta
		String outputFileName = outputDir.substring(0, outputDir.lastIndexOf("/")); // Obtén la ruta sin el nombre del archivo

		// Crear directorios si no existen
		Files.createDirectories(Paths.get(outputFileName)); // Solo la ruta sin el archivo
		Files.write(Paths.get(outputDir + ".java"), content.getBytes()); // Asegúrate de agregar ".java"
	}
}
