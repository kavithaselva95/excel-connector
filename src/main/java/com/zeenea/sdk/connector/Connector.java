
package com.zeenea.sdk.connector;

public interface Connector {
    Connection open(Configuration config) throws Exception;
}
