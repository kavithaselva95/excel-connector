
package com.zeenea.sdk.connector;

import java.io.Closeable;

public interface Connection extends Closeable {
    java.util.List<com.zeenea.sdk.model.Dataset> synchronize() throws Exception;
}
