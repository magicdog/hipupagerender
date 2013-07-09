package com.hipu.render.servlet;

import com.hipu.render.pool.BrowserPool;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class PoolInfoServlet extends HttpServlet {
	private static final Logger LOG = Logger.getLogger(PoolInfoServlet.class);

	private BrowserPool browserPool;

    public PoolInfoServlet(BrowserPool browserPool) {
        super();
        this.browserPool = browserPool;
    }

    @Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

        String res = "{\"availableBrowsers\":"
                + browserPool.availableBrowsers()
                + "}";

        resp.setHeader("Cache-control", "max-age=10");
        resp.setContentType("application/json");


        resp.getWriter().print(res);

		resp.getWriter().close();
	}
}
