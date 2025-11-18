
package com.zeenea.connector.excel.demo;

import java.io.File;

import com.zeenea.sdk.connector.Configuration;
import com.zeenea.sdk.connector.Connector;
import com.zeenea.connector.excel.ExcelConnector;
import com.zeenea.sdk.model.Dataset;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalRunner.class);

    public static void main(String[] args) throws Exception {
        String dir = args.length > 0 ? args[0] : "./test-data";
        Configuration config = new Configuration();
        config.put("directoryPath", dir);
        Connector connector = new ExcelConnector();
        try (var conn = connector.open(config)) {
            List<Dataset> ds = conn.synchronize();
            com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
            m.findAndRegisterModules();
            LOGGER.info("Writing {} datasets to output.json", ds.size());
            m.writerWithDefaultPrettyPrinter()
                    .writeValue(new File("output.json"), ds);
            LOGGER.info("Successfully wrote output.json");
        } catch (Exception e) {
            LOGGER.error("Error during synchronization or writing output", e);
            throw e;
        }
    }
}
