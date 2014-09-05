/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.web.rest.server.api.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bonitasoft.console.common.server.preferences.constants.WebBonitaConstantsUtils;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.document.ArchivedDocumentsSearchDescriptor;
import org.bonitasoft.engine.bpm.document.Document;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.web.rest.model.document.DocumentDefinition;
import org.bonitasoft.web.rest.model.document.DocumentItem;
import org.bonitasoft.web.rest.model.identity.UserItem;
import org.bonitasoft.web.rest.server.api.ConsoleAPI;
import org.bonitasoft.web.rest.server.api.deployer.DeployerFactory;
import org.bonitasoft.web.rest.server.api.document.api.impl.DocumentDatastore;
import org.bonitasoft.web.rest.server.datastore.organization.UserDatastore;
import org.bonitasoft.web.rest.server.framework.search.ItemSearchResult;
import org.bonitasoft.web.rest.server.framework.utils.SearchOptionsBuilderUtil;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIException;
import org.bonitasoft.web.toolkit.client.data.APIID;
import org.bonitasoft.web.toolkit.client.data.item.Definitions;
import org.bonitasoft.web.toolkit.client.data.item.ItemDefinition;

/**
 * @author Julien Mege, Fabio Lombardi
 */
public class APIDocument extends ConsoleAPI<DocumentItem> {
	 
		@Override
	    protected ItemDefinition defineItemDefinition() {
	        return Definitions.get(DocumentDefinition.TOKEN);
	    }

	    @Override
	    public DocumentItem get(final APIID id) {
	        return getDocumentDatastore().get(id);
	    }

	    @Override
	    public DocumentItem add(final DocumentItem item) {
	        return getDocumentDatastore().add(item);
	    }

	    @Override
		public DocumentItem update(APIID id, Map<String, String> attributes) {
			return getDocumentDatastore().update(id, attributes);
		}

		@Override
	    public String defineDefaultSearchOrder() {
	        return "";
	    }

	    @Override
	    protected void fillDeploys(final DocumentItem item, final List<String> deploys) {
	    	addDeployer(getDeployerFactory().createUserDeployer(DocumentItem.ATTRIBUTE_SUBMITTED_BY_USER_ID));
	        addDeployer(getDeployerFactory().createUserDeployer(DocumentItem.DOCUMENT_AUTHOR));
	        super.fillDeploys(item, deploys);
	    }

	    protected DeployerFactory getDeployerFactory() {
	    	return new DeployerFactory(getEngineSession());
	    }

	    @Override
	    protected void fillCounters(final DocumentItem item, final List<String> counters) {
	    }
	    
	    @Override
	    public ItemSearchResult<DocumentItem> search(final int page, final int resultsByPage, final String search, final String orders,
	            final Map<String, String> filters) {

	        final ItemSearchResult<DocumentItem> results = getDocumentDatastore().search(page, resultsByPage, search, filters, orders);

	        return results;
	    }
	    
	    protected DocumentDatastore getDocumentDatastore() {
	        ProcessAPI processAPI;
	        try {
	            processAPI = TenantAPIAccessor.getProcessAPI(getEngineSession());
	        } catch (final Exception e) {
	            throw new APIException(e);
	        }
	        final WebBonitaConstantsUtils constants = WebBonitaConstantsUtils.getInstance(getEngineSession().getTenantId());

	        return new DocumentDatastore(getEngineSession(), constants, processAPI);
	    }
}
