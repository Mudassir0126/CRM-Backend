/**
 * One key/value settings row in the workspace_settings table.
 * The salesperson list is stored as JSON text; the workspace name is stored as plain text.
 */
package com.lankaswisstek.salesorder.features.settings.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspace_settings")
public class WorkspaceSettings {
    // Each setting key is unique, so saving the same key updates its existing value.
    @Id public String settingKey;
    // No explicit column length is configured here; JPA uses its default string-column length.
    public String settingValue;
    // Required by JPA when reading a row from the database.
    public WorkspaceSettings() {}
    // Convenience constructor used when services save one named setting.
    public WorkspaceSettings(String key, String value) { this.settingKey = key; this.settingValue = value; }
}
