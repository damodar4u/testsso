package com.example;

import com.microsoft.aad.msal4j.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;

@WebServlet(name = "AuthServlet", urlPatterns = "/auth/redirect")
public class AuthServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String code = req.getParameter("code");
        if (code == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No authorization code received");
            return;
        }

        try {
            ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                    Config.get("aad.clientId"),
                    ClientCredentialFactory.createFromSecret(Config.get("aad.clientSecret")))
                    .authority("https://login.microsoftonline.com/" + Config.get("aad.tenantId"))
                    .build();

            AuthorizationCodeParameters params = AuthorizationCodeParameters.builder(
                    code,
                    URI.create(Config.get("aad.redirectUri")))
                    .scopes(Collections.singleton("https://graph.microsoft.com/.default"))
                    .build();

            IAuthenticationResult result = app.acquireToken(params).get();
            req.getSession().setAttribute("roles", result.claims().get("roles"));
            resp.sendRedirect("/home.jsp");
        } catch (Exception e) {
            throw new ServletException("Token exchange failed", e);
        }
    }
}
