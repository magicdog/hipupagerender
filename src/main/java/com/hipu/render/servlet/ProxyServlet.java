package com.hipu.render.servlet;

import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpURI;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

/**
 * @author weijian
 *         Date : 2013-06-02 21:18
 */

public class ProxyServlet implements Servlet {
    public static final Logger LOG = Logger.getLogger(ProxyServlet.class);


    protected ServletConfig _config;
    protected ServletContext _context;

//	private PageRender mainrender;

    public ProxyServlet() {
        super();
//        this.mainrender = new PageRender();
    }



    @Override
    public void init(ServletConfig config) throws ServletException {
        _config = config;
        _context = config.getServletContext();
    }

    @Override
    public ServletConfig getServletConfig() {
        return _config;
    }


    /* ------------------------------------------------------------ */
    protected HttpURI proxyHttpURI(HttpServletRequest request, String uri) throws MalformedURLException
    {
        return proxyHttpURI(request.getScheme(), request.getServerName(), request.getServerPort(), uri);
    }

    protected HttpURI proxyHttpURI(String scheme, String serverName, int serverPort, String uri) throws MalformedURLException
    {
        return new HttpURI(scheme + "://" + serverName + ":" + serverPort + uri);
    }


    @Override
    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {
        final int debug = LOG.isDebugEnabled()?req.hashCode():0;

        final HttpServletRequest request = (HttpServletRequest)req;
        final HttpServletResponse response = (HttpServletResponse)res;


        System.out.println(request);
        final InputStream in = request.getInputStream();
//        final OutputStream out = response.getOutputStream();

        String uri = request.getRequestURI();
        System.out.println(uri);
        if (request.getQueryString() != null)
            uri += "?" + request.getQueryString();

        HttpURI url = proxyHttpURI(request,uri);

        if (debug != 0)
            LOG.debug(debug + " proxy " + uri + "-->" + url);

        if (url == null)
        {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        System.out.println(url);

//        String html = mainrender.load(url.toString());
        response.setHeader("Cache-control", "max-age=10");
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/html");

        response.getWriter().print(url.toString());
        response.getWriter().close();
    }


    @Override
    public String getServletInfo() {
        return "Render Servlet";
    }

    @Override
    public void destroy() {

    }
}
