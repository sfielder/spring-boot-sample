package com.example;

// import org.springframework.web.*;
// import javax.servlet.*;
// import org.springframework.web.context.support.*;
//
// public class Application implements WebApplicationInitializer {
//
//   @Override
//   public void onStartup(ServletContext container) {
//     // Create the 'root' Spring application context
//       AnnotationConfigWebApplicationContext rootContext =
//         new AnnotationConfigWebApplicationContext();
//       rootContext.register(AppConfig.class);
//
//       // Manage the lifecycle of the root application context
//       container.addListener(new ContextLoaderListener(rootContext));
//
//       // Create the dispatcher servlet's Spring application context
//       AnnotationConfigWebApplicationContext dispatcherContext =
//         new AnnotationConfigWebApplicationContext();
//       dispatcherContext.register(DispatcherConfig.class);
//
//       // Register and map the dispatcher servlet
//       ServletRegistration.Dynamic dispatcher =
//         container.addServlet("dispatcher", new DispatcherServlet(dispatcherContext));
//       dispatcher.setLoadOnStartup(1);
//       dispatcher.addMapping("/");
//   }
//
// }

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
