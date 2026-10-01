package com.example.MyProject.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import jakarta.annotation.PostConstruct;

/**
 * Serves product images from /images/**.
 *
 * There are two sources, both mapped to the same URL prefix so existing
 * product.imageUrl values (and the frontend's productImageUrl() helper)
 * don't need to know or care which one a given file came from:
 *   1. classpath:/static/images/  - the original seed images bundled into
 *      the build (T-Shirt.jpg, iphone15.jpg, etc.)
 *   2. file:${app.upload.dir}/    - images admins actually upload at
 *      runtime via POST /api/admin/products/upload-image. This is a real
 *      filesystem directory outside the build output, so uploads persist
 *      across restarts and don't require a rebuild to show up (unlike
 *      classpath resources, which are baked in at build time).
 *
 * IMPORTANT: if app.upload.dir is a relative path (the default,
 * "uploads/images"), it resolves relative to wherever the app's working
 * directory happens to be when it starts - and that folder is created
 * fresh, empty, the first time anyone uploads an image. It is NOT part of
 * the project source and never will be included in any zip/build, since it
 * only exists at runtime. If you ever delete a project folder and
 * re-extract a fresh copy (or move the project elsewhere), any previously
 * uploaded images are gone, because that folder never traveled with the
 * source in the first place. If this matters to you, set app.upload.dir in
 * application.properties to an ABSOLUTE path outside your project folder
 * entirely (e.g. app.upload.dir=C:/ecommerce-uploads on Windows, or
 * /var/ecommerce-uploads on Mac/Linux) so it survives no matter how many
 * times the project folder itself gets replaced.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Value("${app.upload.dir:uploads/images}")
    private String uploadDir;

    @PostConstruct
    public void logUploadLocation() {
        String resolved = new File(uploadDir).getAbsolutePath();
        boolean exists = new File(resolved).exists();
        log.info("Product image uploads directory resolved to: {} (exists: {})", resolved, exists);
        if (!new File(uploadDir).isAbsolute()) {
            log.warn("app.upload.dir ('{}') is a RELATIVE path - it depends on the app's working " +
                    "directory and will NOT survive deleting/re-extracting the project folder. " +
                    "Set an absolute path in application.properties if uploaded images need to persist " +
                    "across fresh extractions.", uploadDir);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = new File(uploadDir).getAbsolutePath();
        registry.addResourceHandler("/images/**")
                .addResourceLocations(
                        "classpath:/static/images/",
                        "file:" + uploadPath + "/"
                );
    }
}
