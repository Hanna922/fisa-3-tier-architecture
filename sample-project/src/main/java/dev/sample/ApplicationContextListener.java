package dev.sample;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import dev.sample.config.AppConfig;

@WebListener
public class ApplicationContextListener implements ServletContextListener {

    private AnnotationConfigApplicationContext beanContainer;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();

        beanContainer = new AnnotationConfigApplicationContext(AppConfig.class);
        ctx.setAttribute("BEAN_CONTAINER", beanContainer);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (beanContainer != null)
            beanContainer.close(); // destroyMethod = "close" 덕분에 DS, JedisPool 자동 종료
    }
    
    public static AnnotationConfigApplicationContext getBeanContainer(ServletContext ctx) {
    	return (AnnotationConfigApplicationContext) ctx.getAttribute("BEAN_CONTAINER");
    }
}