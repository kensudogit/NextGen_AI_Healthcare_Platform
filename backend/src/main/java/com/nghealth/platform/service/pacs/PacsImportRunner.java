package com.nghealth.platform.service.pacs;

import com.nghealth.platform.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Order(100)
public class PacsImportRunner {

    private static final Logger log = LoggerFactory.getLogger(PacsImportRunner.class);

    private final AppProperties appProperties;
    private final PacsExportImporter exportImporter;

    public PacsImportRunner(AppProperties appProperties, PacsExportImporter exportImporter) {
        this.appProperties = appProperties;
        this.exportImporter = exportImporter;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void importConfiguredExportFolder() {
        String configured = appProperties.storage().pacsExportPath();
        if (configured == null || configured.isBlank()) {
            return;
        }
        Path root = Paths.get(configured);
        if (!root.toFile().isDirectory()) {
            log.info("PACS export path not found, skipping import: {}", configured);
            return;
        }
        try {
            var result = exportImporter.importFolder(root);
            log.info("PACS export import: {} studies imported, {} skipped (path={})",
                    result.get("imported"), result.get("skipped_existing"), configured);
        } catch (Exception e) {
            log.warn("PACS export import failed for {}: {}", configured, e.getMessage());
        }
    }
}
