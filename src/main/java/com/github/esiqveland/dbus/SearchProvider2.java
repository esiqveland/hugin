package com.github.esiqveland.dbus;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import java.util.List;
import java.util.Map;

@DBusInterfaceName("org.gnome.Shell.SearchProvider2")
public interface SearchProvider2 extends DBusInterface {
    @DBusMemberName("GetResultMetas")
    List<Map<String, Variant<?>>> GetResultMetas(List<String> identifiers);

    @DBusMemberName("GetInitialResultSet")
    List<String> GetInitialResultSet(List<String> terms);

    @DBusMemberName("GetSubsearchResultSet")
    List<String> GetSubsearchResultSet(List<String> previousResults, List<String> terms);

    //@MethodNoReply
    @DBusMemberName("ActivateResult")
    void ActivateResult(String identifier, List<String> terms, UInt32 timestamp);

    //@MethodNoReply
    @DBusMemberName("LaunchSearch")
    void LaunchSearch(List<String> terms, UInt32 timestamp);
}
