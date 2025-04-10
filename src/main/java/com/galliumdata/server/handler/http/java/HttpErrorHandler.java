// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.handler.http.java;

import jakarta.servlet.ServletException;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

public class HttpErrorHandler extends ErrorHandler
{
    public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        final String message = (String)request.getAttribute("jakarta.servlet.error.message");
        if (message != null) {
            response.setHeader("Content-type", "application/json");
            response.getWriter().print(message);
        }
    }
}
