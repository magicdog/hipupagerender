package com.hipu.render.servlet;

import com.hipu.render.pool.BrowserPool;
import org.apache.log4j.Logger;
import org.eclipse.jetty.util.ajax.JSON;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


public class ScreenAllServlet extends HttpServlet {
	private static final Logger LOG = Logger.getLogger(ScreenAllServlet.class);

	private BrowserPool browserPool;

    public ScreenAllServlet(BrowserPool browserPool) {
        super();
        this.browserPool = browserPool;
    }

    @Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
        List<String> base64List = null;
        try {
            base64List = browserPool.screenAllBrowser();

        } catch (Exception e) {
            throw new ServletException(e);
        }


        resp.setHeader("Cache-control", "max-age=10");
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/json");


        resp.getWriter().print(JSON.toString(base64List));

		resp.getWriter().close();
	}
}
