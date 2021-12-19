package com.github.esiqveland.dbus;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;

public class DbusService implements AutoCloseable {

    private final DBusConnection connection;
    private final SearchProvider2 searchProvider;

    public DbusService(SearchProvider2 searchProvider) throws DBusException {
        this.searchProvider = searchProvider;

        connection = DBusConnection.getConnection(DBusConnection.DBusBusType.SESSION);
        // Request a unique bus name
        connection.requestBusName("com.github.esiqveland.Hugin.SearchProvider");
        // Expose searchProvider onto the bus using the object path
        connection.exportObject(this.searchProvider);
    }


    @Override
    public void close() throws Exception {
        try {
            connection.unExportObject(this.searchProvider.getObjectPath());
        } finally {
            connection.close();
        }
    }
}
