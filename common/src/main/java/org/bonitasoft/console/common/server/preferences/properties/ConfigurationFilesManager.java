/**
 * Copyright (C) 2016 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/

package org.bonitasoft.console.common.server.preferences.properties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.bonitasoft.console.common.server.preferences.constants.WebBonitaConstantsUtils;
import org.bonitasoft.console.common.server.utils.PlatformManagementUtils;
import org.bonitasoft.engine.exception.BonitaException;

/**
 * @author Baptiste Mesta
 */
public class ConfigurationFilesManager {

    private static ConfigurationFilesManager INSTANCE = new ConfigurationFilesManager();

    public static ConfigurationFilesManager getInstance() {
        return INSTANCE;
    }

    private static Logger LOGGER = Logger.getLogger(ConfigurationFilesManager.class.getName());

    /*
     * Map<tenantId, Map<propertiesFileName, Properties>>
     */
    private Map<Long, Map<String, Properties>> tenantsConfigurations = new HashMap<>();
    private Map<Long, Map<String, File>> tenantsConfigurationFiles = new HashMap<>();
    private Map<String, Properties> platformConfigurations = new HashMap<>();
    private Map<String, File> platformConfigurationFiles = new HashMap<>();

    public Properties getPlatformProperties(String propertiesFile) {
        Properties properties = platformConfigurations.get(propertiesFile);
        if (properties == null) {
            return new Properties();
        }
        return properties;
    }

    Properties getAlsoCustomAndInternalPropertiesFromFilename(Map<String, Properties> propertiesByFilename, String propertiesFileName) {
        Properties properties = new Properties();
        if (propertiesByFilename != null) {
            if (propertiesByFilename.containsKey(propertiesFileName)) {
                properties.putAll(propertiesByFilename.get(propertiesFileName));
            }
            // if -internal properties also exists, merge key/value pairs:
            final String internalFilename = getSuffixedPropertyFilename(propertiesFileName, "-internal");
            if (propertiesByFilename.containsKey(internalFilename)) {
                properties.putAll(propertiesByFilename.get(internalFilename));
            }
            // if -custom properties also exists, merge key/value pairs (and overwrite previous values if same key name):
            final String customFilename = getSuffixedPropertyFilename(propertiesFileName, "-custom");
            if (propertiesByFilename.containsKey(customFilename)) {
                properties.putAll(propertiesByFilename.get(customFilename));
            }
        }
        return properties;
    }

    public Properties getTenantProperties(String propertiesFile, long tenantId) {
        return getAlsoCustomAndInternalPropertiesFromFilename(tenantsConfigurations.get(tenantId), propertiesFile);
    }

    private Properties getProperties(byte[] content) throws IOException {
        Properties properties = new Properties();
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            properties.load(inputStream);
        }
        return properties;
    }

    public void setPlatformConfigurations(Map<String, byte[]> configurationFiles) throws IOException {
        platformConfigurations = new HashMap<>(configurationFiles.size());
        for (Map.Entry<String, byte[]> entry : configurationFiles.entrySet()) {
            if (entry.getKey().endsWith(".properties")) {
                platformConfigurations.put(entry.getKey(), getProperties(entry.getValue()));
            } else {
                File file = new File(WebBonitaConstantsUtils.getInstance().getTempFolder(), entry.getKey());
                FileUtils.writeByteArrayToFile(file, entry.getValue());
                platformConfigurationFiles.put(entry.getKey(), file);
            }
        }
    }

    public void setTenantConfigurations(Map<String, byte[]> configurationFiles, long tenantId) throws IOException {
        Map<String, Properties> tenantProperties = new HashMap<>();
        Map<String, File> tenantFiles = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : configurationFiles.entrySet()) {
            if (entry.getKey().endsWith(".properties")) {
                tenantProperties.put(entry.getKey(), getProperties(entry.getValue()));
            } else {
                File file = new File(WebBonitaConstantsUtils.getInstance(tenantId).getTempFolder(), entry.getKey());
                FileUtils.writeByteArrayToFile(file, entry.getValue());
                tenantFiles.put(entry.getKey(), file);
            }
        }
        tenantsConfigurations.put(tenantId, tenantProperties);
        tenantsConfigurationFiles.put(tenantId, tenantFiles);
    }

    public void setTenantConfiguration(String fileName, byte[] content, long tenantId) throws IOException {
        if (fileName.endsWith(".properties")) {
            Map<String, Properties> tenantConfiguration = tenantsConfigurations.get(tenantId);
            if (tenantConfiguration != null) {
                tenantConfiguration.put(fileName, getProperties(content));
            }
        } else {
            Map<String, File> tenantConfigurationFiles = tenantsConfigurationFiles.get(tenantId);
            if (tenantConfigurationFiles != null) {
                File file = new File(WebBonitaConstantsUtils.getInstance(tenantId).getTempFolder(), fileName);
                FileUtils.writeByteArrayToFile(file, content);
                tenantConfigurationFiles.put(fileName, file);
            }
        }
    }

    public void removeProperty(String propertiesFilename, long tenantId, String propertyName) throws IOException {
        Map<String, Properties> resources = getResources(tenantId);
        // Now internal behavior stores and removes from -internal file:
        final String internalFilename = getSuffixedPropertyFilename(propertiesFilename, "-internal");
        Properties properties = resources.get(internalFilename);
        if (properties != null) {
            properties.remove(propertyName);
            update(tenantId, internalFilename, properties);
        } else {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.log(Level.FINER, "File " + internalFilename + " not found. Cannot remove property '" + propertyName + "'.");
            }
        }
    }

    private String getSuffixedPropertyFilename(String propertiesFilename, String suffix) {
        return propertiesFilename.replaceAll("\\.properties$", suffix + ".properties");
    }

    private void update(long tenantId, String propertiesFilename, Properties properties) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            properties.store(byteArrayOutputStream, "");
            getPlatformManagementUtils().updateConfigurationFile(tenantId, propertiesFilename, byteArrayOutputStream.toByteArray());
        } catch (BonitaException e) {
            throw new IOException(e);
        }
    }

    PlatformManagementUtils getPlatformManagementUtils() {
        return new PlatformManagementUtils();
    }

    Map<String, Properties> getResources(long tenantId) {
        Map<String, Properties> resources;
        if (tenantId > 0) {
            resources = tenantsConfigurations.get(tenantId);
        } else {
            resources = platformConfigurations;
        }
        return resources;
    }

    public void setProperty(String propertiesFilename, long tenantId, String propertyName, String propertyValue) throws IOException {
        Map<String, Properties> resources = getResources(tenantId);
        // Now internal behavior stores and removes from -internal file:
        final String internalFilename = getSuffixedPropertyFilename(propertiesFilename, "-internal");
        Properties properties = resources.get(internalFilename);
        if (properties != null) {
            properties.setProperty(propertyName, propertyValue);
            update(tenantId, internalFilename, properties);
        } else {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.log(Level.FINER, "File " + internalFilename + " not found. Cannot remove property '" + propertyName + "'.");
            }
        }
    }

    public File getPlatformConfigurationFile(String fileName) {
        return platformConfigurationFiles.get(fileName);
    }

    public File getTenantConfigurationFile(String fileName, long tenantId) {
        Map<String, File> tenantConfigurationFiles = tenantsConfigurationFiles.get(tenantId);
        if (tenantConfigurationFiles != null) {
            return tenantConfigurationFiles.get(fileName);
        }
        return null;
    }

    public File getTenantAutoLoginConfiguration(long tenantId) {
        return getTenantConfigurationFile(PlatformManagementUtils.AUTOLOGIN_V6_JSON, tenantId);
    }
}
