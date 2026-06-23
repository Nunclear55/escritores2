package com.nunclear.escritores.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI/Swagger para la aplicación Escritores.  Al incluir la
 * dependencia {@code springdoc-openapi-starter-webmvc-ui} en el {@code pom.xml},
 * Spring Boot genera automáticamente la especificación OpenAPI 3 para
 * todos los controladores REST presentes en el proyecto.  Esta clase
 * añade metadatos básicos (título, descripción, versión) a dicha
 * especificación.  Una vez que la aplicación esté en ejecución, la
 * documentación estará disponible en <pre>/swagger-ui.html</pre> o
 * <pre>/swagger-ui/index.html</pre>, y la especificación en formato JSON
 * se podrá obtener en <pre>/v3/api-docs</pre> y en YAML en
 * <pre>/v3/api-docs.yaml</pre>.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Define un bean {@link OpenAPI} con información personalizada.  Al
     * declarar este bean, SpringDoc añade los datos definidos en el objeto
     * {@link Info} al documento generado, ayudando a los consumidores de la
     * API a identificar rápidamente el propósito y la versión del servicio.
     *
     * @return una instancia de {@link OpenAPI} con metadatos de la API
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Escritores API")
                        .version("v1")
                        .description("Documentación interactiva de la API de Escritores, generada automáticamente con Swagger/OpenAPI.")
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"))
                );
    }
}