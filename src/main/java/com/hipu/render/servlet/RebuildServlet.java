package com.hipu.render.servlet;

import com.hipu.render.pool.BrowserPool;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class RebuildServlet extends HttpServlet {
	private static final Logger LOG = Logger.getLogger(RebuildServlet.class);

	private BrowserPool browserPool;

    public RebuildServlet(BrowserPool browserPool) {
        super();
        this.browserPool = browserPool;
    }

    @Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

         browserPool.rebuildAllBrowser();

        resp.setHeader("Cache-control", "max-age=10");
        resp.setContentType("application/json");


        resp.getWriter().print("{\"result\": \"success\"");

		resp.getWriter().close();
	}
}
