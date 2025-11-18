
package com.zeenea.connector.excel;

import com.zeenea.sdk.connector.Configuration;
import com.zeenea.sdk.connector.Connection;
import com.zeenea.sdk.model.Dataset;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ExcelConnection implementing L1, L2, L3 requirements.
 */
public class ExcelConnection implements Connection {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelConnection.class);
    private final Path directory;

    public ExcelConnection(Configuration config) {
        String p = config.get("directoryPath");
        if (p == null) throw new IllegalArgumentException("directoryPath required");
        this.directory = Paths.get(p);
        if (!Files.isDirectory(directory) || !Files.isReadable(directory)) {
            throw new IllegalArgumentException("directoryPath must be readable");
        }
    }

    @Override
    public List<Dataset> synchronize() throws Exception {
        List<Dataset> out = new ArrayList<>();
        try (var stream = Files.walk(directory)) {
            List<Path> files = stream.filter(f -> f.toString().toLowerCase().endsWith(".xlsx"))
                    .collect(Collectors.toList());
            for (Path file : files) {
                processFile(file, out);
            }
        }
        LOGGER.info("Processed {} datasets", out.size());
        return out;
    }

    private void processFile(Path file, List<Dataset> out) {
        LOGGER.info("Processing file {}", file.getFileName());
        try (InputStream is = Files.newInputStream(file); Workbook workbook = WorkbookFactory.create(is)) {
            long fileSize = Files.size(file);
            Instant lm = Files.getLastModifiedTime(file).toInstant();

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName();
                int lastRow = sheet.getLastRowNum();
                Row headerRow = sheet.getRow(0);
                int cols = headerRow == null ? 0 : headerRow.getLastCellNum();
                int dataRowCount = Math.max(0, lastRow); // approximate

                if (dataRowCount < 1) {
                    LOGGER.warn("Skipping sheet {}: only {} rows", sheetName, dataRowCount);
                    continue;
                }

                Dataset ds = new Dataset();
                ds.name = file.getFileName().toString().replace(".xlsx","") + " - " + sheetName;
                ds.description = String.format("Sheet '%s' in '%s' (%d rows, %d columns)", sheetName, file.getFileName().toString(), dataRowCount+1, cols);
                ds.properties.put("last_modified", lm.toString());
                ds.properties.put("file_size_bytes", fileSize);

                List<String> headers = extractHeaders(headerRow, cols);
                // For each column, infer type and profile
                for (int c = 0; c < headers.size(); c++) {
                    String header = headers.get(c);
                    Dataset.Field f = new Dataset.Field();
                    f.name = header;
                    f.type = inferType(sheet, c);
                    Map<String,Object> props = profileField(sheet, c, f.type);
                    f.properties.putAll(props);
                    // lineage: basic - find formulas in column and add sourceFields (simplified)
                    Set<Dataset.ItemReference> refs = parseLineage(sheet, c, headers);
                    for (Dataset.ItemReference ir : refs) {
                        f.sourceFields.add(ir);
                    }
                    ds.addField(f);
                }

                out.add(ds);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to read file {}", file.getFileName(), e);
        }
    }

    private List<String> extractHeaders(Row headerRow, int cols) {
        List<String> headers = new ArrayList<>();
        for (int c = 0; c < cols; c++) {
            String h = "";
            if (headerRow != null) {
                Cell cell = headerRow.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                h = cell.toString().trim();
            }
            if (h.isEmpty()) h = "Column_" + (c+1);
            headers.add(h);
        }
        return headers;
    }

    // Basic inference per requirement (first 10 data rows)
    private String inferType(Sheet sheet, int colIndex) {
        int checked = 0;
        int longCount = 0, doubleCount = 0, dateCount = 0;
        for (int r = 1; r <= sheet.getLastRowNum() && checked < 10; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) continue;
            String s = cell.toString().trim();
            if (s.isEmpty()) continue;
            checked++;
            try { Long.parseLong(s); longCount++; continue; } catch (Exception ignored) {}
            try { Double.parseDouble(s); doubleCount++; continue; } catch (Exception ignored) {}
            if (s.matches("\\d{4}-\\d{2}-\\d{2}")) dateCount++;
        }
        if (checked == 0) return "STRING";
        if (longCount >= 0.7 * checked) return "BIGINT";
        if (doubleCount >= 0.7 * checked) return "DOUBLE";
        if (dateCount > 0.5 * checked) return "TIMESTAMP";
        return "STRING";
    }

    // Profile up to 1000 rows
    private Map<String,Object> profileField(Sheet sheet, int colIndex, String inferredType) {
        int maxRows = 1000;
        long rowCount = 0;
        long nullCount = 0;
        Set<String> distinct = new HashSet<>();
        Double sum = 0.0;
        Double min = null, max = null;
        List<String[]> previewRows = new ArrayList<>();
        for (int r = 1; r <= sheet.getLastRowNum() && rowCount < maxRows; r++) {
            Row row = sheet.getRow(r);
            if (row == null) { nullCount++; rowCount++; continue; }
            Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            String v = (cell == null) ? "" : cell.toString().trim();
            if (v.isEmpty()) nullCount++; else distinct.add(v);
            if (previewRows.size() < 3) {
                // collect full row preview
                int cols = row.getLastCellNum();
                String[] preview = new String[cols];
                for (int ci = 0; ci < cols; ci++) {
                    Cell pc = row.getCell(ci, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    preview[ci] = (pc == null) ? "" : pc.toString();
                }
                previewRows.add(preview);
            }
            if (!v.isEmpty()) {
                try {
                    double d = Double.parseDouble(v);
                    sum += d;
                    if (min == null || d < min) min = d;
                    if (max == null || d > max) max = d;
                } catch (Exception ignored) {}
            }
            rowCount++;
        }
        Map<String,Object> props = new HashMap<>();
        double null_pct = rowCount==0?0.0: (double)nullCount / (double)rowCount * 100.0;
        double distinct_pct = rowCount==0?0.0: (double)distinct.size() / (double)rowCount * 100.0;
        props.put("null_pct", Math.round(null_pct*100.0)/100.0);
        props.put("distinct_pct", Math.round(distinct_pct*100.0)/100.0);
        props.put("min_value", min==null?null:String.format("%s", min));
        props.put("max_value", max==null?null:String.format("%s", max));
        props.put("avg_value", (min==null && max==null)?null: (sum / Math.max(1.0, (double)(rowCount - nullCount))));
        props.put("row_count", rowCount);
        // sample_preview JSON string
        try {
            com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
            String previewJson = m.writeValueAsString(previewRows);
            props.put("sample_preview", previewJson);
        } catch (Exception e) {
            props.put("sample_preview", "[]");
        }
        return props;
    }

    // Simplified lineage: scan for formulas in the same row (any column) and find references to columns by letter
    private Set<com.zeenea.sdk.model.Dataset.ItemReference> parseLineage(Sheet sheet, int targetCol, List<String> headers) {
        Set<com.zeenea.sdk.model.Dataset.ItemReference> refs = new HashSet<>();
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell == null) continue;
                if (cell.getCellType() == CellType.FORMULA) {
                    String formula = cell.getCellFormula();
                    // crude parse: find tokens like A1, B2, Sheet2!A1, A1:B3
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("('.*?'|[A-Za-z0-9_]+)?!?\\$?[A-Za-z]+\\$?\\d+(:\\$?[A-Za-z]+\\$?\\d+)?");
                    java.util.regex.Matcher m = p.matcher(formula);
                    while (m.find()) {
                        String ref = m.group();
                        // strip sheet if present
                        if (ref.contains("!")) ref = ref.substring(ref.indexOf("!")+1);
                        // for ranges A1:B3, take left side A1
                        if (ref.contains(":")) ref = ref.split(":")[0];
                        // remove $ and digits to get column letter
                        String colLetters = ref.replaceAll("[\\$\\d]", "");
                        int colIdx = columnLetterToIndex(colLetters);
                        if (colIdx >=0 && colIdx < headers.size()) {
                            com.zeenea.sdk.model.Dataset.ItemReference ir = new com.zeenea.sdk.model.Dataset.ItemReference();
                            //ir.datasetId.put("file", sheet.getWorkbook().getWorkbookName()==null? "unknown" : sheet.getWorkbook().getWorkbookName());
                            ir.datasetId.put("sheet", sheet.getSheetName());
                            ir.fieldName = headers.get(colIdx);
                            refs.add(ir);
                        }
                    }
                }
            }
        }
        return refs;
    }

    private int columnLetterToIndex(String s) {
        s = s.replaceAll("\\$", "").toUpperCase();
        int result = 0;
        for (int i = 0; i < s.length(); i++) {
            result = result * 26 + (s.charAt(i) - 'A' + 1);
        }
        return result - 1;
    }

    @Override
    public void close() throws IOException {
        // no-op
    }
}
