/**
 * Copyright (C) 2013 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
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
package org.bonitasoft.console.common.server.login.filter;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.bonitasoft.console.common.server.api.token.APIToken;
import org.bonitasoft.console.common.server.login.PortalCookies;
import org.bonitasoft.console.common.server.preferences.properties.PropertiesFactory;

/**
 * @author Julien Reboul
 *
 */
public class TokenGenerator {

    protected static final Logger LOGGER = Logger.getLogger(TokenGenerator.class.getName());

    public static final String API_TOKEN = "api_token";
    public static final String X_BONITA_API_TOKEN = "X-Bonita-API-Token";

    /**
     * set the CSRF security token to the HTTP response as cookie.
     */
    public void setTokenToResponseCookie(HttpServletRequest request, HttpServletResponse res, Object apiTokenFromClient) {
        String path = System.getProperty("bonita.csrf.cookie.path", request.getContextPath());
        invalidatePreviousCookie(request, res, path);

        Cookie csrfCookie = new Cookie(X_BONITA_API_TOKEN, apiTokenFromClient.toString());
        // cookie path can be set via system property.
        // Can be set to '/' when another app is deployed in same server than bonita and want to share csrf cookie
        csrfCookie.setPath(path);
        csrfCookie.setSecure(isCSRFTokenCookieSecure());
        res.addCookie(csrfCookie);
    }

    // when a cookie already exists on a different path than the one expected, we need to invalidate it.
    // since there is no way of knowing the path as it is not sent server-side (getPath return null) we invalidate any cookie found
    // see https://bonitasoft.atlassian.net/browse/BS-15883 and BS-16241
    private void invalidatePreviousCookie(HttpServletRequest request, HttpServletResponse res, String path) {
        Cookie cookie = PortalCookies.getCookie(request, X_BONITA_API_TOKEN);
        if (cookie != null) {
            cookie.setMaxAge(0);
            cookie.setValue("");
            res.addCookie(cookie);
            invalidateRootCookie(res, cookie, path);
        }
    }

    //Studio sets the cookie to the path '/' so this is required when using a bunble after a studio in the same browser
    public void invalidateRootCookie(HttpServletResponse res, Cookie cookie, String path) {
        if (!"/".equals(path)) {
            Cookie rootCookie = (Cookie) cookie.clone();
            rootCookie.setPath("/");
            res.addCookie(rootCookie);
        }
    }

    // protected for testing
    protected boolean isCSRFTokenCookieSecure() {
        return PropertiesFactory.getSecurityProperties().isCSRFTokenCookieSecure();
    }
    
    /**
     * generate and store the CSRF security inside HTTP session
     * or retrieve it token from the HTTP session
     *
     * @param req the HTTP session
     * @return the CSRF security token
     */
    public String createOrLoadToken(final HttpSession session) {
        Object apiTokenFromClient = session.getAttribute(API_TOKEN);
        if (apiTokenFromClient == null) {
            apiTokenFromClient = new APIToken().getToken();
            session.setAttribute(API_TOKEN, apiTokenFromClient);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Bonita API Token generated: " + apiTokenFromClient);
            }
        }
        return apiTokenFromClient.toString();
    }

    /**
     * set the CSRF security token to the HTTP response as HTTP Header.
     *
     * @param res the http response
     * @param apiTokenFromClient the security token to set
     */
    public void setTokenToResponseHeader(final HttpServletResponse res, final String token) {
        if(res.containsHeader(X_BONITA_API_TOKEN)){
            res.setHeader(X_BONITA_API_TOKEN, token);
        } else {
            res.addHeader(X_BONITA_API_TOKEN, token);
        }
    }

}
