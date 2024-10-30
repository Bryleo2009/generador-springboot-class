package com.ofsystem.generador;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

@SpringBootApplication
public class GeneradorApplication implements CommandLineRunner {

	private String basePackage;
	private String pkType;

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

		basePackage = getBasePackage(entityPath);

		generateClasses(entityPath);
	}

	private void generateClasses(String entityPath) {
		try {
			String className = getClassName(entityPath);
			String classNameLower = className.toLowerCase();

			// Lee los campos de la entidad
			List<String> fields = getEntityFields(entityPath);

			// Construye el contenido del DTO
			String fieldDefinitions = fields.stream()
					.map(field -> "    private " + field + ";")
					.collect(Collectors.joining("\n"));

			String fieldSetters = fields.stream()
					.map(field -> "                ." + field.split(" ")[1] + "(this." +
							field.split(" ")[1] + ")")
					.collect(Collectors.joining("\n"));


			String fieldGetters = fields.stream()
					.map(field -> "                ." + field.split(" ")[1] + "(" + classNameLower + ".get" +
							field.split(" ")[1].substring(0, 1).toUpperCase() + field.split(" ")[1].substring(1) + "())")
					.collect(Collectors.joining("\n"));

			// Generar las clases usando las plantillas
			createClassFromTemplate("src/main/resources/templates/controller-template.txt",
					basePackage + "/Controller/" + className + "Controller.java",
					className, classNameLower, null, null, null);

			createClassFromTemplate("src/main/resources/templates/dao-template.txt",
					basePackage + "/Dao/" + className + "Dao.java",
					className, classNameLower, null, null, null);

			createClassFromTemplate("src/main/resources/templates/repo-template.txt",
					basePackage + "/Repo/I" + className + "Repo.java",
					className, classNameLower, null, null, null);

			createClassFromTemplate("src/main/resources/templates/service-template.txt",
					basePackage + "/Service/I" + className + "Service.java",
					className, classNameLower, null, null, null);

			// Generar DTO usando plantilla
			createClassFromTemplate("src/main/resources/templates/dto-template.txt",
					basePackage + "/Dto/" + className + "Dto.java",
					className, classNameLower, fieldDefinitions, fieldSetters, fieldGetters);

			// Mostrar rutas generadas
			System.out.println("Clases generadas correctamente para " + className);
			System.out.println("Rutas de los archivos generados:");
			System.out.println(basePackage + "/Controller/" + className + "Controller.java");
			System.out.println(basePackage + "/Dao/" + className + "Dao.java");
			System.out.println(basePackage + "/Repo/I" + className + "Repo.java");
			System.out.println(basePackage + "/Service/I" + className + "Service.java");
			System.out.println(basePackage + "/Dto/" + className + "Dto.java");

		} catch (Exception e) {
			System.err.println("Error al generar clases: " + e.getMessage());
		}
	}

	private List<String> getEntityFields(String entityPath) throws IOException {
		// Extrae los campos del archivo de la entidad
		return Files.lines(Paths.get(entityPath))
				.filter(line -> line.contains("private")) // Filtros básicos
				.map(line -> line.trim().substring(8).replace(";", ""))
				.collect(Collectors.toList());
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

		String[] segments = packagePath.split("/");

		if (segments.length >= 3) {
			return segments[0] + "." + segments[1] + "." + segments[2];
		} else if (segments.length == 2) {
			return segments[0] + "." + segments[1];
		}

		throw new IllegalArgumentException("La ruta de la entidad no contiene un paquete válido.");
	}

	private void createClassFromTemplate(String templatePath, String outputPath, String className, String classNameLower,
										 String fieldDefinitions, String fieldSetters, String fieldGetters) throws IOException {
		// Lee y reemplaza la plantilla
		String template = new String(Files.readAllBytes(Paths.get(templatePath)));
		String content = template
				.replace("{{className}}", className)
				.replace("{{classNameLower}}", classNameLower)
				.replace("{{pkType}}", pkType)
				.replace("{{basePackage}}", basePackage);

		if (fieldDefinitions != null) {
			content = content.replace("{{fields}}", fieldDefinitions)
					.replace("{{fieldSetters}}", fieldSetters)
					.replace("{{fieldGetters}}", fieldGetters);
		}

		//de outputPath elimina .java
		outputPath = outputPath.replace(".java", "");

		// Crea los directorios y escribe el archivo
		String outputDir = "src/main/java/" + outputPath.replace(".", "/");
		//agrga .java
		outputDir = outputDir + ".java";
		Files.createDirectories(Paths.get(outputDir).getParent());
		Files.write(Paths.get(outputDir), content.getBytes());
	}
}
