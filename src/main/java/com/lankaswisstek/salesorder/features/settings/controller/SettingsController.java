/** Settings HTTP routes. Cleaning, defaults, and storage belong to SettingsService. */
package com.lankaswisstek.salesorder.features.settings.controller;

import com.lankaswisstek.salesorder.features.settings.service.SettingsService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final SettingsService service;

    public SettingsController(SettingsService service) { this.service = service; }

    @GetMapping("/sales-people")
    public List<String> getPeople() { return service.getPeople(); }

    @PutMapping("/sales-people")
    public List<String> savePeople(@RequestBody List<String> people) { return service.savePeople(people); }

    @GetMapping("/workspace")
    public Map<String, String> getWorkspace() { return service.getWorkspace(); }

    @PutMapping("/workspace")
    public Map<String, String> saveWorkspace(@RequestBody Map<String, String> body) {
        return service.saveWorkspace(body);
    }
}
