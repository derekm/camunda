/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.operate.webapp.sso;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Profile(SSOWebSecurityConfig.SSO_AUTH_PROFILE)
public class SSOController {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  
  @Autowired
  protected SSOWebSecurityConfig config;
  
  @Autowired
  protected TokenAuthentication tokenAuthentication; 

  /**
   * login the user - the user authentication will be delegated to auth0
   * @param req
   * @return a redirect command to auth0 authorize url 
   */
  @RequestMapping(value = SSOWebSecurityConfig.LOGIN_RESOURCE, method = { RequestMethod.GET, RequestMethod.POST })
  public String login(final HttpServletRequest req) {
    String authorizeUrl = tokenAuthentication.getAuthorizeUrl(req, getRedirectURI(req, SSOWebSecurityConfig.CALLBACK_URI));
    logger.debug("Redirect Login to {}", authorizeUrl);
    return "redirect:" + authorizeUrl;
  }

  /**
   * Logged in callback -  Is called by auth0 with results of user authentication (GET) <br/>
   * Redirects to root url if successful, otherwise it will redirected to an error url.
   * @param req
   * @param res
   * @throws ServletException
   * @throws IOException
   */
  @RequestMapping(value = SSOWebSecurityConfig.CALLBACK_URI, method = RequestMethod.GET)
  public void loggedInCallback(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
    logger.debug("Called back by auth0.");
    try {
      tokenAuthentication.authenticate(req);        
      res.sendRedirect(SSOWebSecurityConfig.ROOT);
    } catch (InsufficientAuthenticationException iae) {
      logoutAndRedirectToNoPermissionPage(req, res);
    } catch (Throwable t /*AuthenticationException | IdentityVerificationException e*/) {
      clearContextAndRedirectToNoPermission(res, t);
    }
  }

  /**
   * Is called when there was an in authentication or authorization
   * @return
   */
  @RequestMapping(value = SSOWebSecurityConfig.NO_PERMISSION)
  @ResponseBody
  public String noPermissions() {
    return "No Permission for Operate - Please check your operate configuration or cloud configuration.";
  }

  /**
   * Logout - Invalidates session and logout from auth0, after that redirects to root url.
   * @param req
   * @param res
   * @throws IOException
   */
  @RequestMapping(value = SSOWebSecurityConfig.LOGOUT_RESOURCE)
  public void logout(HttpServletRequest req, HttpServletResponse res) throws IOException {
    logger.debug("logout user");
    if (req.getSession() != null) {
      req.getSession().invalidate();
    }
    SecurityContextHolder.clearContext();
    logoutFromAuth0(res, getRedirectURI(req, SSOWebSecurityConfig.ROOT));
  }

  protected void clearContextAndRedirectToNoPermission(HttpServletResponse res, Throwable t) throws IOException {
    logger.error("Error in authentication callback: ", t);
    SecurityContextHolder.clearContext();
    res.sendRedirect(SSOWebSecurityConfig.NO_PERMISSION);
  }

  protected void logoutAndRedirectToNoPermissionPage(HttpServletRequest req, HttpServletResponse res) throws IOException {
    logger.error("User is authenticated but there are no permissions. Show noPermission message");
    if (req.getSession() != null) {
      req.getSession().invalidate();
    }
    SecurityContextHolder.clearContext();
    logoutFromAuth0(res, getRedirectURI(req, SSOWebSecurityConfig.NO_PERMISSION));
  }
  
  public String getLogoutUrlFor(String returnTo) {
    return String.format("https://%s/v2/logout?client_id=%s&returnTo=%s", config.getDomain(), config.getClientId(), returnTo);
  }

  protected void logoutFromAuth0(HttpServletResponse res, String returnTo) throws IOException {
    res.sendRedirect(getLogoutUrlFor(returnTo));
  }

  protected String getRedirectURI(final HttpServletRequest req, final String redirectTo) {
    String redirectUri = req.getScheme() + "://" + req.getServerName();
    if ((req.getScheme().equals("http") && req.getServerPort() != 80) || (req.getScheme().equals("https") && req.getServerPort() != 443)) {
      redirectUri += ":" + req.getServerPort();
    }
    return redirectUri + redirectTo;
  }

}