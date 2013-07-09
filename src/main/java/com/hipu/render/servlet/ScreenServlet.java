package com.hipu.render.servlet;

import com.hipu.render.pool.BrowserPool;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class ScreenServlet extends HttpServlet {
	private static final Logger LOG = Logger.getLogger(ScreenServlet.class);

	private BrowserPool browserPool;

    public ScreenServlet(BrowserPool browserPool) {
        super();
        this.browserPool = browserPool;
    }

    @Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
        String url = req.getParameter("url").trim();
        String res = "";

        try {
            String base64 = browserPool.loadScreenshot(url);

            res = "<img src=\"data:image/jpg;base64," + base64 + "\"/>";
        } catch (Exception e) {
            throw new ServletException(e);
        }


        resp.setHeader("Cache-control", "max-age=10");
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("text/html");


        resp.getWriter().print(res);

		resp.getWriter().close();
	}
}
