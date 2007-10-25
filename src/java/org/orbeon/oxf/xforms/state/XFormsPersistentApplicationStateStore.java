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
package org.orbeon.oxf.xforms.state;

import org.orbeon.oxf.pipeline.api.ExternalContext;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.pipeline.StaticExternalContext;
import org.orbeon.oxf.xforms.XFormsProperties;
import org.orbeon.oxf.xforms.XFormsModelSubmission;
import org.orbeon.oxf.xforms.XFormsSubmissionUtils;
import org.orbeon.oxf.xforms.XFormsUtils;
import org.orbeon.oxf.xforms.processor.XFormsServer;
import org.orbeon.oxf.processor.Datasource;
import org.orbeon.oxf.processor.xmldb.XMLDBProcessor;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.xml.TransformerUtils;
import org.orbeon.oxf.xml.dom4j.LocationDocumentResult;
import org.orbeon.saxon.om.FastStringBuffer;
import org.dom4j.Document;
import org.dom4j.io.DocumentResult;
import org.xml.sax.ContentHandler;

import javax.xml.transform.sax.TransformerHandler;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * This store keeps XFormsState instances into an application store and persists data going over a given size.
 */
public class XFormsPersistentApplicationStateStore extends XFormsStateStore {

    private static final boolean TEMP_PERF_TEST = false;
    private static final int TEMP_PERF_ITERATIONS = 100;
    private static final boolean TEMP_USE_XMLDB = true;

    private static final String PERSISTENT_STATE_STORE_APPLICATION_KEY = "oxf.xforms.state.store.persistent-application-key";
    private static final String XFORMS_STATE_STORE_LISTENER_STATE_KEY = "oxf.xforms.state.store.has-session-listeners-key";

    // For now the driver is not configurable, but everything else (URI, username, password, collection) is configurable in properties
    private static final String EXIST_XMLDB_DRIVER = "org.exist.xmldb.DatabaseImpl";

    public synchronized static XFormsStateStore instance(ExternalContext externalContext) {
        {
            final XFormsStateStore existingStateStore
                    = (XFormsStateStore) externalContext.getAttributesMap().get(PERSISTENT_STATE_STORE_APPLICATION_KEY);

            if (existingStateStore != null)
                return existingStateStore;
        }
        {
            // Create new store
            final XFormsPersistentApplicationStateStore newStateStore = new XFormsPersistentApplicationStateStore();

            // Expire remaining persistent entries with session information
            newStateStore.expireAllPersistentWithSession();
//            newStateStore.expireAllPersistent();

            // Keep new store in application scope
            externalContext.getAttributesMap().put(PERSISTENT_STATE_STORE_APPLICATION_KEY, newStateStore);
            return newStateStore;
        }
    }

    protected int getMaxSize() {
        return XFormsProperties.getApplicationStateStoreSize();
    }

    protected String getStoreDebugName() {
        return "global application";
    }

    protected void persistEntry(StoreEntry storeEntry) {

        if (XFormsServer.logger.isDebugEnabled()) {
            debug("persisting entry for key: " + storeEntry.key + " (" + (storeEntry.value.length() * 2) + " bytes).");
        }

        final ExternalContext externalContext;
        final PipelineContext pipelineContext;
        {
            final StaticExternalContext.StaticContext staticContext = StaticExternalContext.getStaticContext();
            externalContext = staticContext.getExternalContext();
            pipelineContext = staticContext.getPipelineContext();
        }

        if (TEMP_PERF_TEST) {

            // Do the operation TEMP_PERF_ITERATIONS times to test performance
            final long startTime = System.currentTimeMillis();
            for (int i = 0; i < TEMP_PERF_ITERATIONS; i ++) {
                if (TEMP_USE_XMLDB) {
                    persistEntryExistXMLDB(pipelineContext, externalContext, storeEntry);
                } else {
                    persistEntryExistHTTP(pipelineContext, externalContext, storeEntry);
                }
            }
            debug("average write persistence time: " + ((System.currentTimeMillis() - startTime) / TEMP_PERF_ITERATIONS) + " ms." );

        } else {
            if (TEMP_USE_XMLDB) {
                persistEntryExistXMLDB(pipelineContext, externalContext, storeEntry);
            } else {
                persistEntryExistHTTP(pipelineContext, externalContext, storeEntry);
            }
        }
    }

    protected String findOne(String key) {

        final String memoryValue = super.findOne(key);
        if (memoryValue != null) {
            // Try memory first
            return memoryValue;
        } else {
            // Try the persistent cache
            final StoreEntry persistedStoreEntry = findPersistedEntry(key);
            if (persistedStoreEntry != null) {
                // Add the key to the list in memory
                addOne(persistedStoreEntry.key, persistedStoreEntry.value, persistedStoreEntry.isInitialEntry);
                debug("migrated persisted entry for key: " + key);
                return persistedStoreEntry.value;
            } else {
                // Not found
                debug("did not find entry in persistent cache for key: " + key);
                return null;
            }
        }
    }

    private void persistEntryExistXMLDB(PipelineContext pipelineContext, ExternalContext externalContext, StoreEntry storeEntry) {
        final String messageBody = encodeMessageBody(pipelineContext, externalContext, storeEntry);
        try {
            new XMLDBAccessor().storeResource(pipelineContext, new Datasource(EXIST_XMLDB_DRIVER,
                    XFormsProperties.getStoreURI(), XFormsProperties.getStoreUsername(), XFormsProperties.getStorePassword()), XFormsProperties.getStoreCollection(),
                    true, storeEntry.key, messageBody);
        } catch (Exception e) {
            throw new OXFException("Unable to store entry in persistent state store for key: " + storeEntry.key, e);
        }
    }

    private void persistEntryExistHTTP(PipelineContext pipelineContext, ExternalContext externalContext, StoreEntry storeEntry) {
        final String url = "/exist/rest" + XFormsProperties.getStoreCollection() + storeEntry.key;
        final String resolvedURL = externalContext.getResponse().rewriteResourceURL(url, true);

        final byte[] messageBody;
        try {
            messageBody = encodeMessageBody(pipelineContext, externalContext, storeEntry).getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new OXFException(e);// won't happen
        }

        // Put document into external storage
        XFormsModelSubmission.ConnectionResult result = XFormsSubmissionUtils.doRegular(externalContext, "put", resolvedURL, null, null, "application/xml", messageBody, null);
        if (result.resultCode < 200 || result.resultCode >= 300)
            throw new OXFException("Got non-successful return code from store persistence layer: " + result.resultCode);
    }

    /**
     * Remove all persisted entries which have the given session id.
     *
     * @param sessionId     Servlet session id
     */
    private synchronized void expirePersistentBySession(String sessionId) {

        final String query = "xquery version \"1.0\";" +
            "                 declare namespace xmldb=\"http://exist-db.org/xquery/xmldb\";" +
            "                 declare namespace util=\"http://exist-db.org/xquery/util\";" +
            "                 <result>" +
            "                   {" +
            "                     count(for $entry in /entry[session-id = '" + sessionId + "']" +
            "                           return (xmldb:remove(util:collection-name($entry), util:document-name($entry)), ''))" +
            "                   }" +
            "                 </result>";

        final Document result = executeQuery(query);
        final int count = Integer.parseInt(result.getDocument().getRootElement().getStringValue());
        debug("expired " + count + " persistent entries for session (" + sessionId + ").");
    }

    private synchronized void expireAllPersistentWithSession() {

        final String query = "xquery version \"1.0\";" +
            "                 declare namespace xmldb=\"http://exist-db.org/xquery/xmldb\";" +
            "                 declare namespace util=\"http://exist-db.org/xquery/util\";" +
            "                 <result>" +
            "                   {" +
            "                     count(for $entry in /entry[session-id]" +
            "                           return (xmldb:remove(util:collection-name($entry), util:document-name($entry)), ''))" +
            "                   }" +
            "                 </result>";

        final Document result = executeQuery(query);
        final int count = Integer.parseInt(result.getRootElement().getStringValue());
        debug("expired " + count + " persistent entries with session information.");
    }

    public synchronized void expireAllPersistent() {

        final String query = "xquery version \"1.0\";" +
            "                 declare namespace xmldb=\"http://exist-db.org/xquery/xmldb\";" +
            "                 declare namespace util=\"http://exist-db.org/xquery/util\";" +
            "                 <result>" +
            "                   {" +
            "                     count(for $entry in /entry" +
            "                           return (xmldb:remove(util:collection-name($entry), util:document-name($entry)), ''))" +
            "                   }" +
            "                 </result>";

        final Document result = executeQuery(query);
        final int count = Integer.parseInt(result.getRootElement().getStringValue());
        debug("expired " + count + " persistent entries.");
    }

    private Document executeQuery(String query) {

        final PipelineContext pipelineContext;
        {
            final StaticExternalContext.StaticContext staticContext = StaticExternalContext.getStaticContext();
            pipelineContext = staticContext.getPipelineContext();
        }

        final DocumentResult result = new DocumentResult();
        final TransformerHandler identity = TransformerUtils.getIdentityTransformerHandler();
        identity.setResult(result);

        new XMLDBAccessor().query(pipelineContext, new Datasource(EXIST_XMLDB_DRIVER,
                XFormsProperties.getStoreURI(), XFormsProperties.getStoreUsername(), XFormsProperties.getStorePassword()), XFormsProperties.getStoreCollection(),
                true, null, query, null, identity);

        return result.getDocument();
    }

    private String encodeMessageBody(PipelineContext pipelineContext, ExternalContext externalContext, StoreEntry storeEntry) {

        final FastStringBuffer sb = new FastStringBuffer("<entry><key>");
        sb.append(storeEntry.key);
        sb.append("</key><value>");

        // Store the value and make sure it is encrypted as it will be externalized
        final String encryptedValue;
        if (storeEntry.value.startsWith("X3") || storeEntry.value.startsWith("X4")) {
            // Data is currently not encrypted, so encrypt it
            final byte[] decodedValue = XFormsUtils.decodeBytes(pipelineContext, storeEntry.value, XFormsProperties.getXFormsPassword());
            encryptedValue = XFormsUtils.encodeBytes(pipelineContext, decodedValue, XFormsProperties.getXFormsPassword());
        } else {
            // Data is already encrypted
            encryptedValue = storeEntry.value;
        }

        sb.append(encryptedValue);
        sb.append("</value>");

        // Store the session id if any
        final ExternalContext.Session session = externalContext.getSession(false);
        if (session != null) {
            sb.append("<session-id>");
            sb.append(session.getId());
            sb.append("</session-id>");

            // Add session listener if needed (we want to register only one expiration listener per session)
            final Map sessionAttributes = session.getAttributesMap(ExternalContext.Session.APPLICATION_SCOPE);
            if (sessionAttributes.get(XFORMS_STATE_STORE_LISTENER_STATE_KEY) == null) {
                session.addListener(new ExternalContext.Session.SessionListener() {
                    public void sessionDestroyed() {
                        expirePersistentBySession(session.getId());
                    }
                });
                sessionAttributes.put(XFORMS_STATE_STORE_LISTENER_STATE_KEY, "");
            }
        }

        // Store the initial entry flag
        sb.append("<is-initial-entry>");
        sb.append(Boolean.toString(storeEntry.isInitialEntry));
        sb.append("</is-initial-entry></entry>");

        return sb.toString();
    }

    private StoreEntry findPersistedEntry(String key) {

        if (XFormsServer.logger.isDebugEnabled()) {
            debug("finding persisting entry for key: " + key + ".");
        }

        StoreEntry result = null;
        if (TEMP_PERF_TEST) {

            // Do the operation TEMP_PERF_ITERATIONS times to test performance
            final long startTime = System.currentTimeMillis();
            for (int i = 0; i < TEMP_PERF_ITERATIONS; i ++) {
                if (TEMP_USE_XMLDB) {
                    result = findPersistedEntryExistXMLDB(key);
                } else {
                    result = findPersistedEntryExistHTTP(key);
                }
                if (result == null)
                    return null;
            }
            debug("average read persistence time: " + ((System.currentTimeMillis() - startTime) / TEMP_PERF_ITERATIONS) + " ms." );

        } else {
            if (TEMP_USE_XMLDB) {
                result = findPersistedEntryExistXMLDB(key);
            } else {
                result = findPersistedEntryExistHTTP(key);
            }
        }

        return result;
    }

    private StoreEntry findPersistedEntryExistXMLDB(String key) {

        final PipelineContext pipelineContext;
        {
            final StaticExternalContext.StaticContext staticContext = StaticExternalContext.getStaticContext();
            pipelineContext = staticContext.getPipelineContext();
        }

        final LocationDocumentResult documentResult = new LocationDocumentResult();
        final TransformerHandler identity = TransformerUtils.getIdentityTransformerHandler();
        identity.setResult(documentResult);

        try {
            new XMLDBAccessor().getResource(pipelineContext, new Datasource(EXIST_XMLDB_DRIVER,
                    XFormsProperties.getStoreURI(), XFormsProperties.getStoreUsername(), XFormsProperties.getStorePassword()),
                    XFormsProperties.getStoreCollection(), true, key, identity);
        } catch (Exception e) {
            throw new OXFException("Unable to find entry in persistent state store for key: " + key, e);
        }

        final Document document = documentResult.getDocument();
        return getStoreEntryFromDocument(key, document);
    }

    private StoreEntry findPersistedEntryExistHTTP(String key) {

        final ExternalContext externalContext;
        {
            final StaticExternalContext.StaticContext staticContext = StaticExternalContext.getStaticContext();
            externalContext = staticContext.getExternalContext();
        }

        final String url = "/exist/rest" + XFormsProperties.getStoreCollection() + key;
        final String resolvedURL = externalContext.getResponse().rewriteResourceURL(url, true);

        XFormsModelSubmission.ConnectionResult result = XFormsSubmissionUtils.doRegular(externalContext, "get", resolvedURL, null, null, null, null, null);

        if (result.resultCode == 404)
            return null;

        if (result.resultCode < 200 || result.resultCode >= 300)
            throw new OXFException("Got non-successful return code from store persistence layer: " + result.resultCode);

        final Document document = TransformerUtils.readDom4j(result.getResultInputStream(), result.resourceURI);
        return getStoreEntryFromDocument(key, document);
    }

    private StoreEntry getStoreEntryFromDocument(String key, Document document) {
        final String value = document.getRootElement().element("value").getStringValue();
        final boolean isInitialEntry = new Boolean(document.getRootElement().element("is-initial-entry").getStringValue()).booleanValue();

        return new StoreEntry(key, value, isInitialEntry);
    }

    private static class XMLDBAccessor extends XMLDBProcessor {
//        public void update(PipelineContext pipelineContext, Datasource datasource, String collectionName, boolean createCollection, String resourceId, String query) {
//            super.update(pipelineContext, datasource, collectionName, createCollection, resourceId, query);
//        }

        public void query(PipelineContext pipelineContext, Datasource datasource, String collectionName, boolean createCollection, String resourceId, String query, Map namespaceContext, ContentHandler contentHandler) {
            super.query(pipelineContext, datasource, collectionName, createCollection, resourceId, query, namespaceContext, contentHandler);
        }

        protected void getResource(PipelineContext pipelineContext, Datasource datasource, String collectionName, boolean createCollection, String resourceName, ContentHandler contentHandler) {
            super.getResource(pipelineContext, datasource, collectionName, createCollection, resourceName, contentHandler);
        }

        protected void storeResource(PipelineContext pipelineContext, Datasource datasource, String collectionName, boolean createCollection, String resourceName, String document) {
            super.storeResource(pipelineContext, datasource, collectionName, createCollection, resourceName, document);
        }
    }
}
