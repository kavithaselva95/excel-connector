package com.zeenea.connector.excel;

import com.zeenea.sdk.connector.Configuration;
import com.zeenea.sdk.model.Dataset;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for ExcelConnection
 */
public class ExcelConnectionTest {

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
    public void testConstructorWithValidDirectory() throws Exception {
        Configuration config = createConfiguration(tempDir.toString());
        try (ExcelConnection conn = new ExcelConnection(config)) {
            assertNotNull(conn);
        }
    }

    @Test
    public void testConstructorWithMissingDirectoryPath() {
        Configuration config = new Configuration();
        assertThrows(IllegalArgumentException.class, () -> new ExcelConnection(config));
    }

    @Test
    public void testConstructorWithNonExistentDirectory() {
        Configuration config = createConfiguration("/non/existent/path/xyz123");
        assertThrows(IllegalArgumentException.class, () -> new ExcelConnection(config));
    }

    @Test
    public void testSynchronizeEmptyDirectory() throws Exception {
        Configuration config = createConfiguration(tempDir.toString());
        try (ExcelConnection conn = new ExcelConnection(config)) {
            List<Dataset> datasets = conn.synchronize();
            assertNotNull(datasets);
            assertTrue(datasets.isEmpty());
        }
    }

    @Test
    public void testSynchronizeWithSingleExcelFile() throws Exception {
        createTestExcelFile("test.xlsx", "TestSheet",
                new String[]{"Name", "Age", "Salary"},
                new Object[][] {
                    {"Alice", 30, 50000},
                    {"Bob", 25, 45000},
                    {"Charlie", 35, 60000}
                });

        Configuration config = createConfiguration(tempDir.toString());
        try (ExcelConnection conn = new ExcelConnection(config)) {
            List<Dataset> datasets = conn.synchronize();
            assertNotNull(datasets);
            assertEquals(1, datasets.size());

            Dataset dataset = datasets.get(0);
            assertNotNull(dataset);
            assertTrue(dataset.name.contains("test"));
            assertTrue(dataset.name.contains("TestSheet"));
            assertEquals(3, dataset.fields.size());
        }
    }

    @Test
    public void testSynchronizeWithMultipleSheets() throws Exception {
        createTestExcelFileWithMultipleSheets("multi.xlsx",
                new String[]{"Sheet1", "Sheet2"},
                new String[][] {
                    new String[]{"Name", "Age"},
                    new String[]{"Product", "Price"}
                },
                new Object[][][] {
                    new Object[][] {
                        {"Alice", 30},
                        {"Bob", 25}
                    },
                    new Object[][] {
                        {"Laptop", 1000},
                        {"Mouse", 25}
                    }
                });

        Configuration config = createConfiguration(tempDir.toString());
        try (ExcelConnection conn = new ExcelConnection(config)) {
            List<Dataset> datasets = conn.synchronize();
            assertNotNull(datasets);
            assertEquals(2, datasets.size());

            Dataset dataset1 = datasets.get(0);
            assertEquals(2, dataset1.fields.size());
        }
    }

    @Test
    public void testSynchronizeIgnoresNonExcelFiles() throws Exception {
        File txtFile = new File(tempDir.toFile(), "test.txt");
        assertTrue(txtFile.createNewFile());

        Configuration config = createConfiguration(tempDir.toString());
        try (ExcelConnection conn = new ExcelConnection(config)) {
            List<Dataset> datasets = conn.synchronize();
            assertNotNull(datasets);
            assertTrue(datasets.isEmpty());
        }
    }

    @Test
    public void testFieldTypeInference() throws Exception {
        createTestExcelFile("types.xlsx", "TypeTest",
                new String[]{"IntColumn", "DecimalColumn", "DateColumn", "StringColumn"},
                new Object[][] {
                    {100.0, 100.5, "2023-01-01", "text"},
                    {200.0, 200.5, "2023-01-02", "value"},
                    {300.0, 300.5, "2023-01-03", "data"},
                    {400.0, 400.5, "2023-01-04", "more"},
                    {500.0, 500.5, "2023-01-05", "info"},
                    {600.0, 600.5, "2023-01-06", "text"},
                    {700.0, 700.5, "2023-01-07", "value"},
                    {800.0, 800.5, "2023-01-08", "data"},
                    {900.0, 900.5, "2023-01-09", "more"},
                    {1000.0, 1000.5, "2023-01-10", "info"}
                });

        Configuration config = createConfiguration(tempDir.toString());
        try (ExcelConnection conn = new ExcelConnection(config)) {
            List<Dataset> datasets = conn.synchronize();
            assertEquals(1, datasets.size());
            Dataset dataset = datasets.get(0);

            assertNotNull(dataset.fields);
            assertTrue(dataset.fields.size() >= 4);

            // Check that numeric columns are inferred as numeric types
            Dataset.Field intField = dataset.fields.stream()
                    .filter(f -> f.name.equals("IntColumn"))
                    .findFirst()
                    .orElse(null);

            assertNotNull(intField);
            assertTrue(intField.type.equals("BIGINT") || intField.type.equals("DOUBLE"),
                    "IntColumn should be inferred as numeric type, got: " + intField.type);
        }
    }

    @Test
    public void testFieldProfileData() throws Exception {
        createTestExcelFile("profile.xlsx", "ProfileTest",
                new String[]{"Numbers"},
                new Object[][] {
                    {10}, {20}, {30}, {40}, {50}
                });

        Configuration config = createConfiguration(tempDir.toString());
        try (ExcelConnection conn = new ExcelConnection(config)) {
            List<Dataset> datasets = conn.synchronize();
            assertEquals(1, datasets.size());
            Dataset dataset = datasets.get(0);
            Dataset.Field field = dataset.fields.get(0);

            assertNotNull(field.properties);
            assertNotNull(field.properties.get("null_pct"));
            assertNotNull(field.properties.get("row_count"));
            assertNotNull(field.properties.get("min_value"));
            assertNotNull(field.properties.get("max_value"));
        }
    }

    @Test
    public void testDatasetProperties() throws Exception {
        createTestExcelFile("props.xlsx", "PropertyTest",
                new String[]{"Col1"},
                new Object[][] {
                    {"value"}
                });

        Configuration config = createConfiguration(tempDir.toString());
        try (ExcelConnection conn = new ExcelConnection(config)) {
            List<Dataset> datasets = conn.synchronize();
            assertEquals(1, datasets.size());
            Dataset dataset = datasets.get(0);

            assertNotNull(dataset.properties);
            assertNotNull(dataset.properties.get("last_modified"));
            assertNotNull(dataset.properties.get("file_size_bytes"));
        }
    }

    @Test
    public void testColumnLetterConversion() throws Exception {
        Configuration config = createConfiguration(tempDir.toString());
        try (ExcelConnection conn = new ExcelConnection(config)) {
            java.lang.reflect.Method method = ExcelConnection.class
                    .getDeclaredMethod("columnLetterToIndex", String.class);
            method.setAccessible(true);

            assertEquals(0, (int) method.invoke(conn, "A"));
            assertEquals(1, (int) method.invoke(conn, "B"));
            assertEquals(25, (int) method.invoke(conn, "Z"));
            assertEquals(26, (int) method.invoke(conn, "AA"));
            assertEquals(701, (int) method.invoke(conn, "ZZ"));
        } catch (NoSuchMethodException e) {
            fail("columnLetterToIndex method not found");
        }
    }

    @Test
    public void testHeaderExtraction() throws Exception {
        createTestExcelFile("headers.xlsx", "HeaderTest",
                new String[]{"Header1", "Header2", "", "Header4"},
                new Object[][] {
                    {"data1", "data2", "data3", "data4"}
                });

        Configuration config = createConfiguration(tempDir.toString());
        try (ExcelConnection conn = new ExcelConnection(config)) {
            List<Dataset> datasets = conn.synchronize();
            assertEquals(1, datasets.size());
            Dataset dataset = datasets.get(0);

            boolean hasAutoHeader = dataset.fields.stream()
                    .anyMatch(f -> f.name.startsWith("Column_"));
            assertTrue(hasAutoHeader || dataset.fields.size() == 3);
        }
    }

    @Test
    public void testCloseOperation() throws Exception {
        Configuration config = createConfiguration(tempDir.toString());
        ExcelConnection conn = new ExcelConnection(config);
        assertDoesNotThrow(conn::close);
    }

    /**
     * Helper method to create a test Excel file
     */
    private void createTestExcelFile(String filename, String sheetName,
                                     String[] headers, Object[][] data) throws Exception {
        File file = new File(tempDir.toFile(), filename);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            for (int rowIdx = 0; rowIdx < data.length; rowIdx++) {
                Row row = sheet.createRow(rowIdx + 1);
                for (int colIdx = 0; colIdx < data[rowIdx].length; colIdx++) {
                    Cell cell = row.createCell(colIdx);
                    Object value = data[rowIdx][colIdx];
                    if (value instanceof String) {
                        cell.setCellValue((String) value);
                    } else if (value instanceof Integer) {
                        cell.setCellValue(((Integer) value).doubleValue());
                    } else if (value instanceof Double) {
                        cell.setCellValue((Double) value);
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    /**
     * Helper method to create a test Excel file with multiple sheets
     */
    private void createTestExcelFileWithMultipleSheets(String filename, String[] sheetNames,
                                                        String[][] headers, Object[][][] data) throws Exception {
        File file = new File(tempDir.toFile(), filename);

        try (Workbook workbook = new XSSFWorkbook()) {
            for (int sheetIdx = 0; sheetIdx < sheetNames.length; sheetIdx++) {
                Sheet sheet = workbook.createSheet(sheetNames[sheetIdx]);

                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers[sheetIdx].length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[sheetIdx][i]);
                }

                for (int rowIdx = 0; rowIdx < data[sheetIdx].length; rowIdx++) {
                    Row row = sheet.createRow(rowIdx + 1);
                    for (int colIdx = 0; colIdx < data[sheetIdx][rowIdx].length; colIdx++) {
                        Cell cell = row.createCell(colIdx);
                        Object value = data[sheetIdx][rowIdx][colIdx];
                        if (value instanceof String) {
                            cell.setCellValue((String) value);
                        } else if (value instanceof Integer) {
                            cell.setCellValue(((Integer) value).doubleValue());
                        } else if (value instanceof Double) {
                            cell.setCellValue((Double) value);
                        }
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }
}
