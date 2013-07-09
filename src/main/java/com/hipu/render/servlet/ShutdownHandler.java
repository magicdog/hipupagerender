package com.hipu.render.servlet;

/**
 * @author weijian
 * Date : 2013-04-09 22:00
 */


import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class ShutdownHandler extends AbstractHandler
{
    private static final Logger LOG = Log.getLogger(ShutdownHandler.class);

    private final String _shutdownToken;

    private final Server _server;

    private boolean _exitJvm = false;



    /**
     * Creates a listener that lets the server be shut down remotely (but only from localhost).
     *
     * @param server
     *            the Jetty instance that should be shut down
     * @param shutdownToken
     *            a secret password to avoid unauthorized shutdown attempts
     */
    public ShutdownHandler(Server server, String shutdownToken)
    {
        this._server = server;
        this._shutdownToken = shutdownToken;
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        if (!target.equals("/shutdown"))
        {
            return;
        }

//        if (!request.getMethod().equals("POST"))
//        {
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
//            return;
//        }
        if (!hasCorrectSecurityToken(request))
        {
            LOG.warn("Unauthorized shutdown attempt from " + getRemoteAddr(request));
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
//        if (!requestFromLocalhost(request))
//        {
//            LOG.warn("Unauthorized shutdown attempt from " + getRemoteAddr(request));
//            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
//            return;
//        }

        LOG.info("Shutting down by request from " + getRemoteAddr(request));

        new Thread()
        {
            public void run ()
            {
                try
                {
                    shutdownServer();
                }
                catch (InterruptedException e)
                {
                    LOG.ignore(e);
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Shutting down server",e);
                }
            }
        }.start();
    }

    private boolean requestFromLocalhost(HttpServletRequest request)
    {
        return "127.0.0.1".equals(getRemoteAddr(request));
    }

    protected String getRemoteAddr(HttpServletRequest request)
    {
        return request.getRemoteAddr();
    }

    private boolean hasCorrectSecurityToken(HttpServletRequest request)
    {
        return _shutdownToken.equals(request.getParameter("token"));
    }

    private void shutdownServer() throws Exception
    {
        _server.stop();

        if (_exitJvm)
        {
            System.exit(0);
        }
    }

    public void setExitJvm(boolean exitJvm)
    {
        this._exitJvm = exitJvm;
    }

}
