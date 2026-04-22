package ovo.sypw.onlineexamsystemback.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Startup dependency version checker.
 * Prints the actual runtime version of critical libraries to help diagnose jar conflicts.
 */
@Component
class DependencyVersionChecker {

    private val logger = LoggerFactory.getLogger(DependencyVersionChecker::class.java)

    @PostConstruct
    fun checkVersions() {
        check("commons-io", "org.apache.commons.io.IOUtils")
        check("Apache POI", "org.apache.poi.util.IOUtils")
        check("Apache POI OOXML", "org.apache.poi.xssf.usermodel.XSSFWorkbook")
    }

    private fun check(library: String, className: String) {
        try {
            val clazz = Class.forName(className)
            val codeSource = clazz.protectionDomain?.codeSource
            val location = codeSource?.location?.toString() ?: "unknown"
            val version = clazz.`package`?.implementationVersion ?: "unknown"
            logger.info("[DependencyCheck] {}: version={}, jar={}", library, version, location)
        } catch (e: ClassNotFoundException) {
            logger.warn("[DependencyCheck] {}: class {} not found", library, className)
        }
    }
}
