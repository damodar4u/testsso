package com.example;

import com.microsoft.aad.msal4j.*;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "AuthServlet", urlPatterns = "/auth/redirect")
public class AuthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // Log the incoming request parameters
            System.out.println("DEBUG: Received request at /auth/redirect");
            String code = req.getParameter("code");
            if (code == null || code.isEmpty()) {
                System.out.println("TRACE: No authorization code received in request.");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No authorization code received");
                return;
            }
            System.out.println("TRACE: Authorization code received: " + code);

            // Log the ConfidentialClientApplication setup
            System.out.println("DEBUG: Setting up ConfidentialClientApplication...");
            ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                    Config.get("aad.clientId"),
                    ClientCredentialFactory.createFromSecret(Config.get("aad.clientSecret")))
                    .authority("https://login.microsoftonline.com/" + Config.get("aad.tenantId"))
                    .build();

            // Log the token exchange process
            System.out.println("DEBUG: Exchanging authorization code for token...");
            AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
                    code,
                    URI.create(Config.get("aad.redirectUri")))
                    .scopes(Collections.singleton("https://graph.microsoft.com/.default"))
                    .build();

            IAuthenticationResult result = app.acquireToken(parameters).get();

            // Log the ID token parsing process
            System.out.println("DEBUG: Parsing ID token...");
            SignedJWT idToken = (SignedJWT) JWTParser.parse(result.idToken());
            List<String> roles = idToken.getJWTClaimsSet().getStringListClaim("roles");

            // Log roles and session setup
            System.out.println("TRACE: Roles extracted from ID token: " + roles);
            req.getSession().setAttribute("roles", roles);

            // Redirect to the home page
            System.out.println("DEBUG: Redirecting to home page...");
            resp.sendRedirect("/custom-servlet-app/home.jsp");

        } catch (Exception e) {
            // Log detailed error information
            System.out.println("ERROR: Exception during authentication process.");
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication failed. Check logs for details.");
        }
    }
}
