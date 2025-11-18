package com.zeenea.connector.excel;

import com.zeenea.sdk.connector.Configuration;
import com.zeenea.sdk.connector.Connection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for ExcelConnector
 */
public class ExcelConnectorTest {

    private Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("excel-test-");
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (tempDir != null) {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            // ignore cleanup errors
                        }
                    });
        }
    }

    private Configuration createConfiguration(String directoryPath) {
        Configuration config = new Configuration();
        config.put("directoryPath", directoryPath);
        return config;
    }

    @Test
    public void testOpen() throws Exception {
        Configuration config = createConfiguration(tempDir.toString());
        ExcelConnector connector = new ExcelConnector();

        try (Connection connection = connector.open(config)) {
            assertNotNull(connection);
            assertTrue(connection instanceof ExcelConnection);
        }
    }

    @Test
    public void testOpenWithValidConfiguration() throws Exception {
        Configuration config = createConfiguration(tempDir.toString());
        ExcelConnector connector = new ExcelConnector();

        assertDoesNotThrow(() -> {
            try (Connection conn = connector.open(config)) {
                // success
            }
        });
    }

    @Test
    public void testOpenWithMissingDirectory() {
        Configuration config = new Configuration();
        ExcelConnector connector = new ExcelConnector();

        assertThrows(IllegalArgumentException.class, () -> connector.open(config));
    }

    @Test
    public void testMultipleOpenCalls() throws Exception {
        Configuration config = createConfiguration(tempDir.toString());
        ExcelConnector connector = new ExcelConnector();

        Connection conn1 = connector.open(config);
        Connection conn2 = connector.open(config);

        try {
            assertNotNull(conn1);
            assertNotNull(conn2);
            assertNotSame(conn1, conn2);
        } finally {
            conn1.close();
            conn2.close();
        }
    }

    @Test
    public void testConnectorInstance() {
        ExcelConnector connector = new ExcelConnector();
        assertNotNull(connector);
        assertTrue(connector instanceof com.zeenea.sdk.connector.Connector);
    }
}
