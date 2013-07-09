package com.hipu.render.servlet;

import com.hipu.render.pool.BrowserPool;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class RenderServlet extends HttpServlet {
	private static final Logger LOG = Logger.getLogger(RenderServlet.class);

	private BrowserPool browserPool;

    public RenderServlet(BrowserPool browserPool) {
        super();
        this.browserPool = browserPool;
    }

    @Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
        String url = req.getParameter("url").trim();
        String res = "";


        try {
            res = browserPool.load(url);
        } catch (Exception e) {
            LOG.error(url, e);
        }

        resp.setHeader("Cache-control", "max-age=10");
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("text/html");


        resp.getWriter().print(res);

		resp.getWriter().close();
	}
}
