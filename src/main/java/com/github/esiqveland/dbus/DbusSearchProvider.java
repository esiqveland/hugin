package com.github.esiqveland.dbus;

import com.github.esiqveland.store.SearchIndexStore;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DbusSearchProvider implements SearchProvider2 {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String OBJECT_PATH = "/com/github/esiqveland/Hugin/SearchProvider";

    private final SearchIndexStore store;
    private final List<String> allowedNamespaces;

    public DbusSearchProvider(SearchIndexStore store, List<String> allowedNamespaces) {
        this.store = store;
        this.allowedNamespaces = allowedNamespaces;
    }

    public static Map<String, Variant<?>> getResult(String id) {
        return Map.of(
                "id", new Variant<>(id),
                "name", new Variant<>("display name"),
                "description", new Variant<>("my test results")
        );
    }

    /**
     * GetResultMetas :: (as) → (aa{sv})
     * GetResultMetas is called to obtain detailed information for results.
     * It gets an array of result IDs as arguments, and should return a matching array of dictionaries (ie one a{sv} for each passed-in result ID).
     *
     * The following pieces of information should be provided for each result:
     *
     * “id”: the result ID
     * “name”: the display name for the result
     * “icon”: a serialized GIcon (see g_icon_serialize()), or alternatively,
     * “gicon”: a textual representation of a GIcon (see g_icon_to_string()), or alternativly,
     * “icon-data”: a tuple of type (iiibiiay) describing a pixbuf with width, height, rowstride, has-alpha, bits-per-sample, and image data
     * “description”: an optional short description (1-2 lines)
     *
     * @param identifiers
     * @return
     */
    @Override
    public List<Map<String, Variant<?>>> GetResultMetas(List<String> identifiers) {
        log.info("GetResultMetas terms={}", identifiers);

        return List.of(getResult("eivindtest"));
    }

    /**
     * GetInitialResultSet :: (as) → (as)
     * GetInitialResultSet is called when a new search is started.
     * It gets an array of search terms as arguments, and should return an array of result IDs.
     * gnome-shell will call GetResultMetas for (some) of these result IDs to get details about
     * the result that can be be displayed in the result list.
     *
     * @param terms
     * @return
     */
    @Override
    public List<String> GetInitialResultSet(List<String> terms) {
        log.info("GetInitialResultSet terms={}", terms);

        var res = store.query(new SearchIndexStore.SearchRequest(
                allowedNamespaces,
                terms.get(0)
        )).join();

        log.info("GetInitialResultSet terms={} res={}", terms, res);

        return res.hits().stream().map(s -> s.docId()).toList();
    }

    @Override
    public List<String> GetSubsearchResultSet(List<String> previous_results, List<String> terms) {
        log.info("GetSubsearchResultSet terms={} previous_results={}", terms, previous_results);

        var res = store.query(new SearchIndexStore.SearchRequest(
                allowedNamespaces,
                terms.get(0)
        )).join();

        log.info("GetSubsearchResultSet terms={} previous_results={} res={}", terms, previous_results, res);

        return res.hits().stream().map(s -> s.docId()).toList();
    }

    /**
     * ActivateResult :: (s,as,u) → ()
     * ActivateResult is called when the user clicks on an individual result to open it in the application.
     * The arguments are the result ID, the current search terms and a timestamp.
     * @param terms
     * @param timestamp
     */
    @Override
    public void ActivateResult(String id, List<String> terms, UInt32 timestamp) {
        log.info("ActivateResult terms={} timestamp={}", terms, timestamp);

    }

    @Override
    public void LaunchSearch(List<String> terms, UInt32 timestamp) {
        log.info("LaunchSearch terms={} timestamp={}", terms, timestamp);
        log.info("LaunchSearch terms={} timestamp={}", terms, timestamp);
        log.info("LaunchSearch terms={} timestamp={}", terms, timestamp);
    }

    @Override
    public String getObjectPath() {
        return OBJECT_PATH;
    }


}
