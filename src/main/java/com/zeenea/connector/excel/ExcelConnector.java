
package com.zeenea.connector.excel;

import com.zeenea.sdk.connector.Configuration;
import com.zeenea.sdk.connector.Connector;
import com.zeenea.sdk.connector.Connection;

public class ExcelConnector implements Connector {
    @Override
    public Connection open(Configuration config) throws Exception {
        return new ExcelConnection(config);
    }
}
