# Hugin

Hugin remembers things and finds them again for you.

A toy project to see where I can take indexing content on the linux desktop.

### Notes

A collection of my own helpful notes:

On semantic tags:

https://freedesktop.org/wiki/Specifications/shared-filemetadata-spec/

On using xattrs for indexers:

https://freedesktop.org/wiki/CommonExtendedAttributes/

so we should probably use a uniquely named prefix, such as user.hugin.objectid

### Dbus integration

https://rm5248.com/d-bus-tutorial/

https://dbus.freedesktop.org/doc/dbus-tutorial.html

Writing a Gnome search provider:
https://developer.gnome.org/documentation/tutorials/search-provider.html

Old dbus-java doc:
https://dbus.freedesktop.org/doc/dbus-java/dbus-java/

see also:
https://github.com/hypfvieh/dbus-java/blob/dbus-java-parent-3.3.1/dbus-java-utils/src/main/java/org/freedesktop/dbus/utils/generator/InterfaceCodeGenerator.java

Introspect dbus provider:
```bash

$ dbus-send --print-reply=literal --type=method_call --dest=com.github.esiqveland.Hugin.SearchProvider /com/github/esiqveland/Hugin/SearchProvider org.freedesktop.DBus.Introspectable.Introspect

```
