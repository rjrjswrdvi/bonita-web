/**
 * Copyright (C) 2012 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.console.client.admin.organization.users.action;

import org.bonitasoft.console.client.angular.AngularIFrameView;
import org.bonitasoft.web.toolkit.client.ViewController;
import org.bonitasoft.web.toolkit.client.common.TreeIndexed;
import org.bonitasoft.web.toolkit.client.data.APIID;
import org.bonitasoft.web.toolkit.client.ui.action.ActionOnItemId;
import org.bonitasoft.web.toolkit.client.ui.page.PageOnItem;

public class ShowUserMoreDetailAction extends ActionOnItemId {

    @Override
    protected void execute(APIID userId) {
        final TreeIndexed<String> tree = new TreeIndexed<String>();
        tree.addValue(PageOnItem.PARAMETER_ITEM_ID, userId.toString());
        ViewController.showView(AngularIFrameView.USER_MORE_DETAILS_ADMIN, tree);
    }

}