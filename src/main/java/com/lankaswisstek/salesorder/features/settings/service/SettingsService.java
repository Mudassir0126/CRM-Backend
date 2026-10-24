/** Settings business rules and JSON storage; the controller handles HTTP endpoints. */
package com.lankaswisstek.salesorder.features.settings.service;

import com.lankaswisstek.salesorder.features.settings.model.WorkspaceSettings;
import com.lankaswisstek.salesorder.features.settings.repository.WorkspaceSettingsRepository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class SettingsService {
    // Fixed database keys keep each kind of setting in its own row.
    private static final String PEOPLE_KEY = "salesPeople";
    private static final String WORKSPACE_KEY = "workspaceName";
    private final WorkspaceSettingsRepository settings;
    // Jackson converts the salesperson list between Java objects and stored JSON text.
    private final ObjectMapper mapper;

    /** Spring supplies these dependencies through constructor injection. */
    public SettingsService(WorkspaceSettingsRepository settings, ObjectMapper mapper) {
        this.settings = settings;
        this.mapper = mapper;
    }

    /** GET: read saved names; on first access, create and return the default list. */
    public List<String> getPeople() {
        return settings.findById(PEOPLE_KEY)
                .map(row -> parsePeople(row.settingValue))
                .orElseGet(() -> {
                    List<String> defaults = List.of("Manjunath", "Ashish", "Vijay");
                    savePeople(defaults);
                    return defaults;
                });
    }

    /** PUT: replace all names after trimming whitespace, removing blanks, and deduplicating. */
    public List<String> savePeople(List<String> people) {
        // distinct() compares exact strings, so differently capitalized names remain distinct here.
        List<String> clean = people.stream().map(String::trim).filter(s -> !s.isBlank()).distinct().toList();
        settings.save(new WorkspaceSettings(PEOPLE_KEY, writePeople(clean)));
        return clean;
    }

    /** GET: return an object with companyName, using a display default if no row exists yet. */
    public Map<String, String> getWorkspace() {
        String value = settings.findById(WORKSPACE_KEY).map(row -> row.settingValue).orElse("Lanka Swisstek");
        return Map.of("companyName", value);
    }

    /** PUT: save a nonblank company name or return HTTP 400 for an empty/whitespace value. */
    public Map<String, String> saveWorkspace(Map<String, String> body) {
        String name = body.getOrDefault("companyName", "Lanka Swisstek").trim();
        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace name cannot be blank");
        settings.save(new WorkspaceSettings(WORKSPACE_KEY, name));
        return Map.of("companyName", name);
    }

    /** Decode JSON from the settings row. Malformed stored JSON currently falls back to an empty list. */
    private List<String> parsePeople(String value) {
        try { return mapper.readValue(value, new TypeReference<List<String>>() {}); }
        catch (Exception e) { return new ArrayList<>(); }
    }
    /** Encode the list as JSON text; serialization failure is surfaced as a server-side exception. */
    private String writePeople(List<String> people) {
        try { return mapper.writeValueAsString(people); }
        catch (Exception e) { throw new IllegalStateException("Could not save sales people", e); }
    }
}
