/**
 * Database access for workspace settings. Spring supplies save/findById and other CRUD methods.
 * The String ID is the setting key, for example salesPeople or workspaceName.
 */
package com.lankaswisstek.salesorder.features.settings.repository;

import com.lankaswisstek.salesorder.features.settings.model.WorkspaceSettings;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceSettingsRepository extends JpaRepository<WorkspaceSettings, String> {}
