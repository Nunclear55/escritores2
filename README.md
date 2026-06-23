# Escritores - Plataforma de Historias Colaborativas

Una plataforma profesional y escalable para la gestión y publicación de historias colaborativas, desarrollada con **Spring Boot 4.0**, **Java 21** y **MySQL**.

## Características

✅ **Gestión de Usuarios**
- Registro e inicio de sesión
- Perfiles de usuario con avatar y biografía
- Cambio de contraseña seguro
- Niveles de acceso (administrador, moderador, usuario, lector)

✅ **Gestión de Historias**
- Crear, editar y archivar historias
- Estados de publicación (borrador, publicado, archivado)
- Visibilidad (público, privado, no listado)
- Portadas e imágenes

✅ **Estructura Narrativa**
- Arcos narrativos
- Volúmenes
- Capítulos con contenido extenso
- Estimación automática de tiempo de lectura

✅ **Personajes y Habilidades**
- Registro de personajes con detalles completos
- Sistema de habilidades por personaje
- Proficiencias y roles narrativos

✅ **Sistema de Comentarios**
- Comentarios en historias y capítulos
- Respuestas anidadas
- Ocultamiento de comentarios
- Eliminación lógica

✅ **Sistema de Calificaciones**
- Calificación de historias (1-5 estrellas)
- Reseñas opcionales
- Un voto por usuario por historia
- Cálculo de promedio automático

✅ **Interacción Social**
- Marcar historias como favoritas
- Seguimiento de autores
- Vista de historias (analíticas)

✅ **Moderación**
- Reportes de contenido
- Sanciones a usuarios
- Comunicados globales

## Requisitos Previos

- **Java 21** o superior
- **Maven 3.6+**
- **MySQL 8.0+**
- **Git** (opcional)

## Instalación

### 1. Clonar el Repositorio

```bash
git clone <repository-url>
cd escritores
```

### 2. Crear la Base de Datos

Ejecutar el script SQL proporcionado en `scripts/database-setup.sql`:

```bash
mysql -u root -p < scripts/database-setup.sql
```

O, si prefieres usar una interfaz gráfica como MySQL Workbench o phpMyAdmin, importa el archivo SQL.

### 3. Configurar la Conexión a la Base de Datos

Editar `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/historias_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=tu_contraseña
```

### 4. Compilar el Proyecto

```bash
mvn clean install
```

### 5. Ejecutar la Aplicación

```bash
mvn spring-boot:run
```

O ejecutar directamente el JAR compilado:

```bash
java -jar target/escritores-0.0.1-SNAPSHOT.jar
```

La aplicación estará disponible en `http://localhost:8080`

## Estructura del Proyecto

```
escritores/
├── src/
│   ├── main/
│   │   ├── java/com/nunclear/escritores/
│   │   │   ├── controller/          # Controladores REST
│   │   │   ├── service/             # Servicios de negocio
│   │   │   ├── repository/          # Acceso a datos (JPA)
│   │   │   ├── entity/              # Entidades JPA
│   │   │   ├── dto/                 # DTOs (Data Transfer Objects)
│   │   │   ├── config/              # Configuración
│   │   │   └── exception/           # Manejo de excepciones
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/        # Migraciones de base de datos
│   └── test/
├── pom.xml                          # Dependencias Maven
├── docapi.md                        # Documentación de API
└── README.md                        # Este archivo
```

## Guía de Uso

### Crear un Usuario

**Endpoint:** `POST /api/users/register`

```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "loginName": "usuario1",
    "emailAddress": "usuario@example.com",
    "displayName": "Usuario Ejemplo",
    "password": "password123"
  }'
```

### Crear una Historia

**Endpoint:** `POST /api/stories`

```bash
curl -X POST http://localhost:8080/api/stories \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "title": "Mi Primera Historia",
    "description": "Una historia épica",
    "visibilityState": "public",
    "publicationState": "draft"
  }'
```

### Crear un Capítulo

**Endpoint:** `POST /api/chapters?storyId=1`

```bash
curl -X POST "http://localhost:8080/api/chapters?storyId=1" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "title": "Capítulo 1",
    "subtitle": "El Comienzo",
    "content": "Érase una vez, en un lugar muy lejano...",
    "publicationState": "draft"
  }'
```

### Publicar una Historia

**Endpoint:** `PUT /api/stories/{id}`

```bash
curl -X PUT http://localhost:8080/api/stories/1 \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "publicationState": "published"
  }'
```

### Comentar una Historia

**Endpoint:** `POST /api/comments`

```bash
curl -X POST http://localhost:8080/api/comments \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 2" \
  -d '{
    "storyId": 1,
    "content": "¡Excelente historia!"
  }'
```

### Calificar una Historia

**Endpoint:** `POST /api/ratings`

```bash
curl -X POST http://localhost:8080/api/ratings \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 2" \
  -d '{
    "storyId": 1,
    "scoreValue": 5,
    "reviewText": "Una obra maestra"
  }'
```

## Documentación API

Para una documentación completa de todos los endpoints disponibles, consulta el archivo `docapi.md` en la raíz del proyecto.

## Entidades Principales

### AppUser
- Información del usuario
- Nivel de acceso y estado de cuenta
- Datos de perfil (avatar, biografía)

### Story
- Información de la historia
- Propietario y estado de publicación
- Configuración de comentarios y calificaciones

### Chapter
- Contenido de los capítulos
- Cálculo automático de tiempo de lectura
- Asociación a volúmenes

### Arc & Volume
- Estructura narrativa de la historia
- Ordenamiento jerárquico

### StoryCharacter
- Personajes de la historia
- Habilidades y profesiones
- Roles narrativos

### StoryComment
- Comentarios en historias y capítulos
- Sistema de respuestas anidadas
- Control de visibilidad

### StoryRating
- Calificaciones y reseñas
- Límite de una por usuario por historia

### UserFollow & StoryFavorite
- Sistema social (seguimiento)
- Favoritos de usuarios

### ContentReport & UserSanction
- Reportes de contenido
- Sistema de moderación y sanciones

## Configuración de Seguridad

### Contraseña
Las contraseñas se cifran usando **BCrypt** con un factor de costo de 10 (predeterminado de Spring Security).

### Autorización
El sistema está preparado para futuras implementaciones de:
- JWT (JSON Web Tokens)
- OAuth 2.0
- Roles basados en acceso (RBAC)

### Header de Autenticación
Actualmente utiliza `X-User-Id` para identificar al usuario. Se recomienda migrar a JWT en producción.

## Migraciones de Base de Datos

El proyecto usa **Hibernate** para generar automáticamente el esquema de la base de datos. La configuración `spring.jpa.hibernate.ddl-auto=update` creará y actualizará automáticamente las tablas.

Para migraciones controladas en producción, considera usar **Flyway** o **Liquibase**.

## Manejo de Errores

El sistema incluye validaciones en:
- **Entidades**: Anotaciones `@NotBlank`, `@Email`, `@Min`, `@Max`, etc.
- **Servicios**: Lógica de validación y autorización
- **Controladores**: Manejo de excepciones y respuestas HTTP adecuadas

## Testing

Para ejecutar las pruebas unitarias:

```bash
mvn test
```

## Pruebas de carga con k6

Además de las pruebas unitarias, el proyecto incluye un script de **k6** para medir el rendimiento de la API. k6 es una herramienta de pruebas de carga de código abierto desarrollada por Grafana Labs, diseñada para evaluar el comportamiento de un servicio bajo distintos niveles de concurrencia【91227551196018†L63-L68】. Los scripts de k6 se escriben en JavaScript y se ejecutan con el comando `k6 run archivo.js`【91227551196018†L124-L144】.

### Configuración

1. **Instalar k6:**
   - Consulta la documentación oficial de k6 para instalar el binario en tu sistema (Linux, macOS o Windows).
   - Alternativamente, utiliza la imagen oficial de Docker: `docker pull grafana/k6`.

2. **Definir la URL base:**
   - El script utiliza `http://localhost:8080` como valor por defecto. Puedes sobreescribirlo con la variable de entorno `BASE_URL` al ejecutar la prueba.

### Ejecutar la prueba

El script de k6 se encuentra en `k6/api-load-test.js`. Ejecuta la prueba con uno de estos métodos:

```bash
# Usando k6 instalado en el sistema
k6 run k6/api-load-test.js

# Usando Docker (sin instalar k6 en tu máquina)
docker run --rm -i grafana/k6 run - < k6/api-load-test.js
```

El script por defecto genera una carga escalonada de 5 a 15 usuarios virtuales y realiza peticiones a los endpoints públicos `/stories`, `/v3/api-docs`, `/metrics/stories/top-viewed` y `POST /auth/login`. Puedes ajustar las etapas y los usuarios modificando la propiedad `options.stages` en el archivo `k6/api-load-test.js`.

Al finalizar la ejecución, k6 mostrará un resumen de métricas como el número de peticiones, tiempos de respuesta y porcentaje de errores. Consulta la [documentación de k6](https://k6.io/docs/) para más opciones y configuraciones.

## Logs

Los logs se configuran en `application.properties`. Por defecto:
- Nivel global: **INFO**
- Nivel de aplicación: **DEBUG**
- Nivel de SQL: **DEBUG**

## Performance

El proyecto implementa:
- **Índices** en columnas frecuentemente consultadas
- **Lazy Loading** en relaciones JPA
- **Batch Processing** en Hibernate
- **Compresión de respuestas HTTP**
- **Caché de conexiones** a base de datos

## Escalabilidad Futura

El sistema está preparado para:
- Implementación de caché (Redis)
- Búsqueda avanzada (Elasticsearch)
- Microservicios
- APIs asincrónicas
- Autenticación distribuida (OAuth 2.0)

## Contribuir

Para contribuir al proyecto:

1. Haz un fork del repositorio
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## Licencia

Este proyecto está bajo la Licencia MIT. Ver `LICENSE` para más detalles.

## Contacto

Para preguntas o sugerencias, contacta al equipo de desarrollo.

## Recursos Útiles

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [MySQL Documentation](https://dev.mysql.com/doc/)
- [Hibernate Documentation](https://hibernate.org/)

---

**Versión:** 0.0.1  
**Última actualización:** Abril 2024  
**Estado:** En desarrollo
