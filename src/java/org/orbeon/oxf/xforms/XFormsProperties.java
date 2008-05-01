/**
 *  Copyright (C) 2007 Orbeon, Inc.
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version
 *  2.1 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.xforms;

import org.orbeon.oxf.resources.OXFProperties;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Iterator;

public class XFormsProperties {

    public static final String XFORMS_PROPERTY_PREFIX = "oxf.xforms.";

    // Document properties
    public static final String STATE_HANDLING_PROPERTY = "state-handling";
    public static final String STATE_HANDLING_SERVER_VALUE = "server";
    public static final String STATE_HANDLING_CLIENT_VALUE = "client";
    public static final String STATE_HANDLING_SESSION_VALUE = "session"; // deprecated

    public static final String READONLY_APPEARANCE_PROPERTY = "readonly-appearance";
    public static final String READONLY_APPEARANCE_STATIC_VALUE = "static";
    public static final String READONLY_APPEARANCE_DYNAMIC_VALUE = "dynamic";

    public static final String ORDER_PROPERTY = "order";
    public static final String DEFAULT_ORDER_PROPERTY = "label control help alert hint";

    public static final String EXTERNAL_EVENTS_PROPERTY = "external-events";
    private static final String READONLY_PROPERTY = "readonly";

    private static final String OPTIMIZE_GET_ALL_PROPERTY = "optimize-get-all";
    private static final String OPTIMIZE_LOCAL_SUBMISSION_PROPERTY = "optimize-local-submission";
//    private static final String XFORMS_OPTIMIZE_LOCAL_INSTANCE_LOADS_PROPERTY = "optimize-local-instance-loads";
    private static final String OPTIMIZE_RELEVANCE_PROPERTY = "optimize-relevance";
    private static final String EXCEPTION_ON_INVALID_CLIENT_CONTROL_PROPERTY = "exception-invalid-client-control";
    private static final String AJAX_SHOW_LOADING_ICON_PROPERTY = "ajax.show-loading-icon";
    private static final String AJAX_SHOW_ERRORS_PROPERTY = "ajax.show-errors";

    private static final String MINIMAL_RESOURCES_PROPERTY = "minimal-resources";
    private static final String COMBINE_RESOURCES_PROPERTY = "combine-resources";

    private static final String SKIP_SCHEMA_VALIDATION_PROPERTY = "skip-schema-validation";

    private static final String DATE_FORMAT_PROPERTY = "format.date";
    private static final String DATETIME_FORMAT_PROPERTY = "format.dateTime";
    private static final String TIME_FORMAT_PROPERTY = "format.time";
    private static final String DECIMAL_FORMAT_PROPERTY = "format.decimal";
    private static final String INTEGER_FORMAT_PROPERTY = "format.integer";
    private static final String FLOAT_FORMAT_PROPERTY = "format.float";
    private static final String DOUBLE_FORMAT_PROPERTY = "format.double";

    private static final String SESSION_HEARTBEAT_PROPERTY = "session-heartbeat";
    public static final String SESSION_HEARTBEAT_DELAY_PROPERTY = "session-heartbeat-delay";
    public static final String FCK_EDITOR_BASE_PATH_PROPERTY = "fck-editor-base-path";
    public static final String YUI_BASE_PATH_PROPERTY = "yui-base-path";
    private static final String DELAY_BEFORE_INCREMENTAL_REQUEST_PROPERTY = "delay-before-incremental-request";
    private static final String DELAY_BEFORE_FORCE_INCREMENTAL_REQUEST_PROPERTY = "delay-before-force-incremental-request";
    private static final String DELAY_BEFORE_GECKO_COMMUNICATION_ERROR_PROPERTY = "delay-before-gecko-communication-error";
    private static final String DELAY_BEFORE_CLOSE_MINIMAL_DIALOG_PROPERTY = "delay-before-close-minimal-dialog";
    private static final String DELAY_BEFORE_AJAX_TIMEOUT_PROPERTY = "delay-before-ajax-timeout";
    private static final String INTERNAL_SHORT_DELAY_PROPERTY = "internal-short-delay";
    private static final String DELAY_BEFORE_DISPLAY_LOADING_PROPERTY = "delay-before-display-loading";
    private static final String REQUEST_RETRIES_PROPERTY = "request-retries";
    private static final String DEBUG_WINDOW_HEIGHT_PROPERTY = "debug-window-height";
    private static final String DEBUG_WINDOW_WIDTH_PROPERTY = "debug-window-width";
    private static final String LOADING_MIN_TOP_PADDING_PROPERTY = "loading-min-top-padding";

    private static final String REVISIT_HANDLING_PROPERTY = "revisit-handling";
    public static final String REVISIT_HANDLING_RESTORE_VALUE = "restore";
    public static final String REVISIT_HANDLING_RELOAD_VALUE = "reload";

    public static final String HELP_HANDLER_PROPERTY = "help-handler";
    private static final String HELP_TOOLTIP_PROPERTY = "help-tooltip";
    public static final String OFFLINE_SUPPORT_PROPERTY = "offline";

    private static final String COMPUTED_BINDS_PROPERTY = "computed-binds";
    public static final String COMPUTED_BINDS_RECALCULATE_VALUE = "recalculate";
    public static final String COMPUTED_BINDS_REVALIDATE_VALUE = "revalidate";

    public static class PropertyDefinition {

        private String name;
        private Object defaultValue;
        private boolean isPropagateToClient;

        public PropertyDefinition(String name, String defaultValue, boolean propagateToClient) {
            this.name = name;
            this.defaultValue = defaultValue;
            isPropagateToClient = propagateToClient;
        }

        public PropertyDefinition(String name, boolean defaultValue, boolean propagateToClient) {
            this.name = name;
            this.defaultValue = new Boolean(defaultValue);
            isPropagateToClient = propagateToClient;
        }

        public PropertyDefinition(String name, int defaultValue, boolean propagateToClient) {
            this.name = name;
            this.defaultValue = new Integer(defaultValue);
            isPropagateToClient = propagateToClient;
        }

        public String getName() {
            return name;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public boolean isPropagateToClient() {
            return isPropagateToClient;
        }

        public Object parseProperty(String value) {
            if (getDefaultValue() instanceof Integer) {
                return new Integer(value);
            } else if (getDefaultValue() instanceof Boolean) {
                return new Boolean(value);
            } else {
                return value;
            }
        }
    }

    private static final PropertyDefinition[] SUPPORTED_DOCUMENT_PROPERTIES_DEFAULTS = {
            new PropertyDefinition(STATE_HANDLING_PROPERTY, STATE_HANDLING_SERVER_VALUE, false),
            new PropertyDefinition(READONLY_PROPERTY, false, false),
            new PropertyDefinition(READONLY_APPEARANCE_PROPERTY, READONLY_APPEARANCE_DYNAMIC_VALUE, false),
            new PropertyDefinition(ORDER_PROPERTY, DEFAULT_ORDER_PROPERTY, false),
            new PropertyDefinition(EXTERNAL_EVENTS_PROPERTY, "", false),
            new PropertyDefinition(OPTIMIZE_GET_ALL_PROPERTY, true, false),
            new PropertyDefinition(OPTIMIZE_LOCAL_SUBMISSION_PROPERTY, true, false),
            new PropertyDefinition(OPTIMIZE_RELEVANCE_PROPERTY, false, false),
            new PropertyDefinition(EXCEPTION_ON_INVALID_CLIENT_CONTROL_PROPERTY, false, false),
            new PropertyDefinition(AJAX_SHOW_LOADING_ICON_PROPERTY, true, false),
            new PropertyDefinition(AJAX_SHOW_ERRORS_PROPERTY, true, false),
            new PropertyDefinition(MINIMAL_RESOURCES_PROPERTY, true, false),
            new PropertyDefinition(COMBINE_RESOURCES_PROPERTY, true, false),
            new PropertyDefinition(SKIP_SCHEMA_VALIDATION_PROPERTY, false, false),
            new PropertyDefinition(COMPUTED_BINDS_PROPERTY, COMPUTED_BINDS_RECALCULATE_VALUE, false),
            new PropertyDefinition(DATE_FORMAT_PROPERTY, "if (. castable as xs:date) then format-date(xs:date(.), '[FNn] [MNn] [D], [Y] [ZN]', 'en', (), ()) else .", false),
            new PropertyDefinition(DATETIME_FORMAT_PROPERTY, "if (. castable as xs:dateTime) then format-dateTime(xs:dateTime(.), '[FNn] [MNn] [D], [Y] [H01]:[m01]:[s01] [ZN]', 'en', (), ()) else .", false),
            new PropertyDefinition(TIME_FORMAT_PROPERTY, "if (. castable as xs:time) then format-time(xs:time(.), '[H01]:[m01]:[s01] [ZN]', 'en', (), ()) else .", false),
            new PropertyDefinition(DECIMAL_FORMAT_PROPERTY, "if (. castable as xs:decimal) then format-number(xs:decimal(.),'###,###,###,##0.00') else .", false),
            new PropertyDefinition(INTEGER_FORMAT_PROPERTY, "if (. castable as xs:integer) then format-number(xs:integer(.),'###,###,###,##0') else .", false),
            new PropertyDefinition(FLOAT_FORMAT_PROPERTY, "if (. castable as xs:float) then format-number(xs:float(.),'#,##0.000') else .", false),
            new PropertyDefinition(DOUBLE_FORMAT_PROPERTY, "if (. castable as xs:double) then format-number(xs:double(.),'#,##0.000') else .", false),

            // Properties to propagate to the client
            new PropertyDefinition(SESSION_HEARTBEAT_PROPERTY, true, true),
            new PropertyDefinition(SESSION_HEARTBEAT_DELAY_PROPERTY, 12 * 60 * 60 * 800, true), // dynamic; 80 % of 12 hours in ms
            new PropertyDefinition(FCK_EDITOR_BASE_PATH_PROPERTY, "/ops/fckeditor/", true),// dynamic
            new PropertyDefinition(YUI_BASE_PATH_PROPERTY, "/ops/images/yui/", true),// dynamic
            new PropertyDefinition(DELAY_BEFORE_INCREMENTAL_REQUEST_PROPERTY, 500, true),
            new PropertyDefinition(DELAY_BEFORE_FORCE_INCREMENTAL_REQUEST_PROPERTY, 2000, true),
            new PropertyDefinition(DELAY_BEFORE_GECKO_COMMUNICATION_ERROR_PROPERTY, 5000, true),
            new PropertyDefinition(DELAY_BEFORE_CLOSE_MINIMAL_DIALOG_PROPERTY, 5000, true),
            new PropertyDefinition(DELAY_BEFORE_AJAX_TIMEOUT_PROPERTY, -1, true),
            new PropertyDefinition(INTERNAL_SHORT_DELAY_PROPERTY, 10, true),
            new PropertyDefinition(DELAY_BEFORE_DISPLAY_LOADING_PROPERTY, 500, true),
            new PropertyDefinition(REQUEST_RETRIES_PROPERTY, 3, true),
            new PropertyDefinition(DEBUG_WINDOW_HEIGHT_PROPERTY, 600, true),
            new PropertyDefinition(DEBUG_WINDOW_WIDTH_PROPERTY, 300, true),
            new PropertyDefinition(LOADING_MIN_TOP_PADDING_PROPERTY, 10, true),
            new PropertyDefinition(REVISIT_HANDLING_PROPERTY, REVISIT_HANDLING_RESTORE_VALUE, true),
            new PropertyDefinition(HELP_HANDLER_PROPERTY, false, true),// dynamic
            new PropertyDefinition(HELP_TOOLTIP_PROPERTY, false, true),
            new PropertyDefinition(OFFLINE_SUPPORT_PROPERTY, false, true)// dynamic
    };

    private static final Map SUPPORTED_DOCUMENT_PROPERTIES;
    static {
        final Map tempMap = new HashMap();
        for (int i = 0; i < SUPPORTED_DOCUMENT_PROPERTIES_DEFAULTS.length; i++) {
            final PropertyDefinition propertyDefinition = SUPPORTED_DOCUMENT_PROPERTIES_DEFAULTS[i];
            tempMap.put(propertyDefinition.name, propertyDefinition);
        }
        SUPPORTED_DOCUMENT_PROPERTIES = Collections.unmodifiableMap(tempMap);
    }

    // Global properties
    private static final String PASSWORD_PROPERTY = XFORMS_PROPERTY_PREFIX + "password";
    private static final String CACHE_DOCUMENT_PROPERTY = XFORMS_PROPERTY_PREFIX + "cache.document";
    private static final boolean CACHE_DOCUMENT_DEFAULT = true;

    private static final String STORE_APPLICATION_SIZE_PROPERTY = XFORMS_PROPERTY_PREFIX + "store.application.size";
    private static final int STORE_APPLICATION_SIZE_DEFAULT = 20 * 1024 * 1024;

    private static final String STORE_APPLICATION_USERNAME_PROPERTY = XFORMS_PROPERTY_PREFIX + "store.application.username";
    private static final String STORE_APPLICATION_PASSWORD_PROPERTY = XFORMS_PROPERTY_PREFIX + "store.application.password";
    private static final String STORE_APPLICATION_URI_PROPERTY = XFORMS_PROPERTY_PREFIX + "store.application.uri";
    private static final String STORE_APPLICATION_COLLECTION_PROPERTY = XFORMS_PROPERTY_PREFIX + "store.application.collection";

    private static final String STORE_APPLICATION_USERNAME_DEFAULT = "guest";
    private static final String STORE_APPLICATION_PASSWORD_DEFAULT = "";
    private static final String STORE_APPLICATION_URI_DEFAULT = "xmldb:exist:///";
    private static final String STORE_APPLICATION_COLLECTION_DEFAULT = "/db/orbeon/xforms/cache/";

    private static final String GZIP_STATE_PROPERTY = XFORMS_PROPERTY_PREFIX + "gzip-state"; // global but could possibly be per document
    private static final boolean GZIP_STATE_DEFAULT = true;

    private static final String HOST_LANGUAGE_AVTS_PROPERTY = XFORMS_PROPERTY_PREFIX + "host-language-avts"; // global but should be per document
    private static final boolean HOST_LANGUAGE_AVTS_DEFAULT = false;

    private static final String CACHE_COMBINED_RESOURCES_PROPERTY = XFORMS_PROPERTY_PREFIX + "cache-combined-resources"; // global but could possibly be per document
    private static final boolean CACHE_COMBINED_RESOURCES_DEFAULT = false;

    private static final String TEST_AJAX_PROPERTY = XFORMS_PROPERTY_PREFIX + "test.ajax";
    private static final boolean TEST_AJAX_DEFAULT = false;

    // The following global properties are deprecated in favor of the persistent application store
    private static final String CACHE_SESSION_SIZE_PROPERTY = XFORMS_PROPERTY_PREFIX + "cache.session.size";
    private static final int CACHE_SESSION_SIZE_DEFAULT = 1024 * 1024;
    private static final String CACHE_APPLICATION_SIZE_PROPERTY = XFORMS_PROPERTY_PREFIX + "cache.application.size";
    private static final int CACHE_APPLICATION_SIZE_DEFAULT = 1024 * 1024;

    // Legacy XForms Classic properties
    private static final String ENCRYPT_NAMES_PROPERTY = XFORMS_PROPERTY_PREFIX + "encrypt-names";
    private static final String ENCRYPT_HIDDEN_PROPERTY = XFORMS_PROPERTY_PREFIX + "encrypt-hidden";

    /**
     * Return a PropertyDefinition given a property nam.
     *
     * @param propertyName  property name
     * @return              PropertyDefinition
     */
    public static PropertyDefinition getPropertyDefinition(String propertyName) {
        return (XFormsProperties.PropertyDefinition) SUPPORTED_DOCUMENT_PROPERTIES.get(propertyName);
    }

    /**
     * Return an iterator over property definition entries.
     *
     * @return  Iterator<Entry<String,PropertyDefinition>> mapping a property name to a definition
     */
    public static Iterator getPropertyDefinitionEntryIterator() {
        return SUPPORTED_DOCUMENT_PROPERTIES.entrySet().iterator();
    }

    public  static Object parseProperty(String propertyName, String propertyValue) {
        final PropertyDefinition propertyDefinition = getPropertyDefinition(propertyName);
        return propertyDefinition.parseProperty(propertyValue);
    }

    public static String getXFormsPassword() {
        if (isHiddenEncryptionEnabled())
            return OXFProperties.instance().getPropertySet().getString(PASSWORD_PROPERTY);
        else
            return null;// TODO: is this needed?
    }

    public static boolean isCacheDocument() {
        return OXFProperties.instance().getPropertySet().getBoolean
                (CACHE_DOCUMENT_PROPERTY, CACHE_DOCUMENT_DEFAULT).booleanValue();
    }

    public static int getSessionStoreSize() {
        return OXFProperties.instance().getPropertySet().getInteger
                (CACHE_SESSION_SIZE_PROPERTY, CACHE_SESSION_SIZE_DEFAULT).intValue();
    }

    public static int getApplicationStateStoreSize() {
        return OXFProperties.instance().getPropertySet().getInteger
                (STORE_APPLICATION_SIZE_PROPERTY, STORE_APPLICATION_SIZE_DEFAULT).intValue();
    }

    public static int getApplicationCacheSize() {
        return OXFProperties.instance().getPropertySet().getInteger
                (CACHE_APPLICATION_SIZE_PROPERTY, CACHE_APPLICATION_SIZE_DEFAULT).intValue();
    }

    public static boolean isGZIPState() {
        return OXFProperties.instance().getPropertySet().getBoolean
                (GZIP_STATE_PROPERTY, GZIP_STATE_DEFAULT).booleanValue();
    }

    public static boolean isAjaxTest() {
        return OXFProperties.instance().getPropertySet().getBoolean
                (TEST_AJAX_PROPERTY, TEST_AJAX_DEFAULT).booleanValue();
    }

    public static String getStoreUsername() {
        return OXFProperties.instance().getPropertySet().getString
                (STORE_APPLICATION_USERNAME_PROPERTY, STORE_APPLICATION_USERNAME_DEFAULT);
    }

    public static String getStorePassword() {
        return OXFProperties.instance().getPropertySet().getString
                (STORE_APPLICATION_PASSWORD_PROPERTY, STORE_APPLICATION_PASSWORD_DEFAULT);
    }

    public static String getStoreURI() {
        return OXFProperties.instance().getPropertySet().getStringOrURIAsString
                (STORE_APPLICATION_URI_PROPERTY, STORE_APPLICATION_URI_DEFAULT);
    }

    public static String getStoreCollection() {
        return OXFProperties.instance().getPropertySet().getString
                (STORE_APPLICATION_COLLECTION_PROPERTY, STORE_APPLICATION_COLLECTION_DEFAULT);
    }

    public static boolean isHostLanguageAVTs() {
        return OXFProperties.instance().getPropertySet().getBoolean
                (HOST_LANGUAGE_AVTS_PROPERTY, HOST_LANGUAGE_AVTS_DEFAULT).booleanValue();
    }

    public static boolean isCacheCombinedResources() {
        return OXFProperties.instance().getPropertySet().getBoolean
                (CACHE_COMBINED_RESOURCES_PROPERTY, CACHE_COMBINED_RESOURCES_DEFAULT).booleanValue();
    }

    public static String getStateHandling(XFormsContainingDocument containingDocument) {
        return getStringProperty(containingDocument, STATE_HANDLING_PROPERTY);
    }

    public static boolean isClientStateHandling(XFormsContainingDocument containingDocument) {
        return getStateHandling(containingDocument).equals(STATE_HANDLING_CLIENT_VALUE);
    }

    public static boolean isLegacySessionStateHandling(XFormsContainingDocument containingDocument) {
        return getStateHandling(containingDocument).equals(STATE_HANDLING_SESSION_VALUE);
    }

    public static boolean isOptimizeGetAllSubmission(XFormsContainingDocument containingDocument) {
        return getBooleanProperty(containingDocument, OPTIMIZE_GET_ALL_PROPERTY);
    }

    public static boolean isOptimizeLocalSubmission(XFormsContainingDocument containingDocument) {
        return getBooleanProperty(containingDocument, OPTIMIZE_LOCAL_SUBMISSION_PROPERTY);
    }

    public static boolean isExceptionOnInvalidClientControlId(XFormsContainingDocument containingDocument) {
        return getBooleanProperty(containingDocument, EXCEPTION_ON_INVALID_CLIENT_CONTROL_PROPERTY);
    }

    public static boolean isAjaxShowLoadingIcon(XFormsContainingDocument containingDocument) {
        return getBooleanProperty(containingDocument, AJAX_SHOW_LOADING_ICON_PROPERTY);
    }

    public static boolean isAjaxShowErrors(XFormsContainingDocument containingDocument) {
        return getBooleanProperty(containingDocument, AJAX_SHOW_ERRORS_PROPERTY);
    }

    public static boolean isMinimalResources(XFormsContainingDocument containingDocument) {
        return getBooleanProperty(containingDocument, MINIMAL_RESOURCES_PROPERTY);
    }

    public static boolean isCombinedResources(XFormsContainingDocument containingDocument) {
        return getBooleanProperty(containingDocument, COMBINE_RESOURCES_PROPERTY);
    }

    public static boolean isOptimizeRelevance(XFormsContainingDocument containingDocument) {
        return getBooleanProperty(containingDocument, OPTIMIZE_RELEVANCE_PROPERTY);
    }

    public static boolean isSkipSchemaValidation(XFormsContainingDocument containingDocument) {
        return getBooleanProperty(containingDocument, SKIP_SCHEMA_VALIDATION_PROPERTY);
    }

    public static String getComputedBinds(XFormsContainingDocument containingDocument) {
        return getStringProperty(containingDocument, COMPUTED_BINDS_PROPERTY);
    }

    public static boolean isReadonly(XFormsContainingDocument containingDocument) {
        return getBooleanProperty(containingDocument, READONLY_PROPERTY);
    }

    public static String getReadonlyAppearance(XFormsContainingDocument containingDocument) {
        return getStringProperty(containingDocument, READONLY_APPEARANCE_PROPERTY);
    }

    public static String getOrder(XFormsContainingDocument containingDocument) {
        return getStringProperty(containingDocument, ORDER_PROPERTY);
    }

    public static boolean isStaticReadonlyAppearance(XFormsContainingDocument containingDocument) {
        return getReadonlyAppearance(containingDocument).equals(XFormsProperties.READONLY_APPEARANCE_STATIC_VALUE);
    }

    public static String getDateFormat(XFormsContainingDocument containingDocument) {
        return getStringProperty(containingDocument, DATE_FORMAT_PROPERTY);
    }

    public static String getDateTimeFormat(XFormsContainingDocument containingDocument) {
        return getStringProperty(containingDocument, DATETIME_FORMAT_PROPERTY);
    }

    public static String getTimeFormat(XFormsContainingDocument containingDocument) {
        return getStringProperty(containingDocument, TIME_FORMAT_PROPERTY);
    }

    public static String getDecimalFormat(XFormsContainingDocument containingDocument) {
        return getStringProperty(containingDocument, DECIMAL_FORMAT_PROPERTY);
    }

    public static String getIntegerFormat(XFormsContainingDocument containingDocument) {
        return getStringProperty(containingDocument, INTEGER_FORMAT_PROPERTY);
    }
    public static String getFloatFormat(XFormsContainingDocument containingDocument) {
        return getStringProperty(containingDocument, FLOAT_FORMAT_PROPERTY);
    }

    public static String getDoubleFormat(XFormsContainingDocument containingDocument) {
        return getStringProperty(containingDocument, DOUBLE_FORMAT_PROPERTY);
    }

    public static boolean isSessionHeartbeat(XFormsContainingDocument containingDocument) {
        return getBooleanProperty(containingDocument, SESSION_HEARTBEAT_PROPERTY);
    }

    public static boolean isOfflineMode(XFormsContainingDocument containingDocument) {
        return getBooleanProperty(containingDocument, OFFLINE_SUPPORT_PROPERTY);
    }

    private static boolean getBooleanProperty(XFormsContainingDocument containingDocument, String propertyName) {
        if (containingDocument.getStaticState() != null)
            return containingDocument.getStaticState().getBooleanProperty(propertyName);
        else // case of legacy XForms engine which doesn't have a static state object
            return OXFProperties.instance().getPropertySet().getBoolean(propertyName, ((Boolean) (XFormsProperties.getPropertyDefinition(propertyName)).getDefaultValue()).booleanValue()).booleanValue();
    }

    private static String getStringProperty(XFormsContainingDocument containingDocument, String propertyName) {
        if (containingDocument.getStaticState() != null)
            return containingDocument.getStaticState().getStringProperty(propertyName);
        else // case of legacy XForms engine which doesn't have a static state object
            return OXFProperties.instance().getPropertySet().getString(propertyName, (XFormsProperties.getPropertyDefinition(propertyName)).getDefaultValue().toString());
    }

    /**
     * @return  whether name encryption is enabled (legacy XForms engine only).
     */
    public static boolean isNameEncryptionEnabled() {
        return OXFProperties.instance().getPropertySet().getBoolean
                (ENCRYPT_NAMES_PROPERTY, false).booleanValue();
    }

    /**
     * @return  whether hidden fields encryption is enabled (legacy XForms engine only).
     */
    public static boolean isHiddenEncryptionEnabled() {
        return OXFProperties.instance().getPropertySet().getBoolean
                (ENCRYPT_HIDDEN_PROPERTY, false).booleanValue();
    }
}
