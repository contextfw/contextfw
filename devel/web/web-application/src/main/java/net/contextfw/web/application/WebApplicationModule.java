package net.contextfw.web.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.contextfw.web.application.component.Component;
import net.contextfw.web.application.internal.ContextPathProvider;
import net.contextfw.web.application.internal.WebApplicationServletModule;
import net.contextfw.web.application.internal.component.AutoRegisterListener;
import net.contextfw.web.application.internal.initializer.InitializerProvider;
import net.contextfw.web.application.internal.providers.HttpContextProvider;
import net.contextfw.web.application.internal.providers.RequestProvider;
import net.contextfw.web.application.internal.providers.WebApplicationHandleProvider;
import net.contextfw.web.application.internal.scope.WebApplicationScope;
import net.contextfw.web.application.internal.service.WebApplicationContextHandler;
import net.contextfw.web.application.internal.util.AttributeHandler;
import net.contextfw.web.application.internal.util.ClassScanner;
import net.contextfw.web.application.internal.util.ObjectAttributeSerializer;
import net.contextfw.web.application.lifecycle.PageFlowFilter;
import net.contextfw.web.application.lifecycle.PageScoped;
import net.contextfw.web.application.lifecycle.View;
import net.contextfw.web.application.properties.KeyValue;
import net.contextfw.web.application.properties.Properties;
import net.contextfw.web.application.serialize.AttributeJsonSerializer;
import net.contextfw.web.application.util.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

public final class WebApplicationModule extends AbstractModule {

    private final Properties configuration;
    
    private final ContextPathProvider contextPathProvider = new ContextPathProvider();
    
    private InitializerProvider initializerProvider;
    
    private Logger logger = LoggerFactory.getLogger(WebApplicationModule.class);

    @SuppressWarnings("rawtypes")
    private AutoRegisterListener autoRegisterListener = new AutoRegisterListener();

    public WebApplicationModule(Properties configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        WebApplicationScope webApplicationScope = new WebApplicationScope();
        bindScope(PageScoped.class, webApplicationScope);

        bind(WebApplicationScope.class).annotatedWith(
                Names.named("webApplicationScope")).toInstance(
                webApplicationScope);

        bind(HttpContext.class).toProvider(HttpContextProvider.class);
        bind(ObjectAttributeSerializer.class).to(AttributeHandler.class);
        bind(WebApplicationHandle.class).toProvider(
                WebApplicationHandleProvider.class);
        bind(Request.class).toProvider(RequestProvider.class);
        bind(InitializerProvider.class).toInstance(configureInitializers());
        bind(Properties.class).toInstance(configuration);
        bind(PropertyProvider.class).to(configuration.get(Properties.PROPERTY_PROVIDER));
        bind(ContextPathProvider.class).toInstance(contextPathProvider);
        
        this.bindListener(Matchers.any(), new TypeListener() {
            @SuppressWarnings("unchecked")
            @Override
            public <I> void hear(TypeLiteral<I> typeLiteral,
                    TypeEncounter<I> typeEncounter) {
                if (Component.class.isAssignableFrom(typeLiteral
                        .getRawType())) {
                    typeEncounter.register(autoRegisterListener);
                }
            }
        });
        
        WebApplicationServletModule servletModule = new WebApplicationServletModule(configuration);
        requestInjection(servletModule);
        install(servletModule);
    }

    @Singleton
    @Provides
    public Gson provideGson(Injector injector) {
        
        GsonBuilder builder = new GsonBuilder();
        
        for (KeyValue<Class<?>, Class<? extends JsonSerializer<?>>> entry : configuration
                .get(Properties.JSON_SERIALIZER)) {
            builder.registerTypeAdapter(entry.getKey(), injector.getInstance(entry.getValue()));
        }
        
        for (KeyValue<Class<?>, Class<? extends JsonDeserializer<?>>> entry : configuration
                .get(Properties.JSON_DESERIALIZER)) {
            builder.registerTypeAdapter(entry.getKey(), injector.getInstance(entry.getValue()));
        }
        
        for (KeyValue<Class<?>, Class<? extends AttributeJsonSerializer<?>>> entry : configuration
                .get(Properties.ATTRIBUTE_JSON_SERIALIZER)) {
            builder.registerTypeAdapter(entry.getKey(), injector.getInstance(entry.getValue()));
        }
        
        return builder.create();
    }
    
    @Singleton
    @Provides
    public WebApplicationContextHandler provideWebApplicationContextHandler(PageFlowFilter pageFlowFilter) {
        final WebApplicationContextHandler handler = new WebApplicationContextHandler(configuration, pageFlowFilter);
        Timer timer = new Timer(true);
        logger.info("Starting scheduled removal for expired web applications");
        
        timer.schedule(new TimerTask() {
            public void run() {
                handler.removeExpiredApplications();
            }
        }, configuration.get(Properties.REMOVAL_SCHEDULE_PERIOD), 
        configuration.get(Properties.REMOVAL_SCHEDULE_PERIOD)); 
        
        return handler;
    }

    @SuppressWarnings("unchecked")
    public void postInitialize(String contextPath) {
    	contextPathProvider.setContextPath(contextPath);
    	initializerProvider.setContextPath(contextPath);
    	List<String> rootPackages = new ArrayList<String>();
        rootPackages.addAll(configuration.get(Properties.VIEW_COMPONENT_ROOT_PACKAGE));
        List<Class<?>> classes = ClassScanner.getClasses(rootPackages);

        for (Class<?> cl : classes) {
            if (Component.class.isAssignableFrom(cl)
                    && cl.getAnnotation(View.class) != null) {
                initializerProvider.addInitializer((Class<? extends Component>) cl);
            }
        }
    }
    
    private InitializerProvider configureInitializers() {
        initializerProvider = new InitializerProvider(configuration);
        return initializerProvider;
    }

	public InitializerProvider getInitializerProvider() {
		return initializerProvider;
	}
}