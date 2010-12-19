package net.contextfw.web.application;

import java.util.List;
import java.util.Map.Entry;

import net.contextfw.web.application.annotations.PageScoped;
import net.contextfw.web.application.component.Component;
import net.contextfw.web.application.converter.ObjectAttributeSerializer;
import net.contextfw.web.application.internal.component.AutoRegisterListener;
import net.contextfw.web.application.internal.initializer.InitializerProvider;
import net.contextfw.web.application.internal.providers.HttpContextProvider;
import net.contextfw.web.application.internal.providers.RequestProvider;
import net.contextfw.web.application.internal.providers.WebApplicationHandleProvider;
import net.contextfw.web.application.internal.scope.WebApplicationScope;
import net.contextfw.web.application.internal.util.AttributeHandler;
import net.contextfw.web.application.internal.util.ClassScanner;
import net.contextfw.web.application.request.Request;
import net.contextfw.web.application.view.View;

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

    private final ModuleConfiguration configuration;

    @SuppressWarnings("rawtypes")
    private AutoRegisterListener autoRegisterListener = new AutoRegisterListener();

    public WebApplicationModule(ModuleConfiguration configuration) {
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
        bind(ModuleConfiguration.class).toInstance(configuration);
        
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
    }

    @Singleton
    @Provides
    public Gson provideGson(Injector injector) {
        GsonBuilder builder = new GsonBuilder();
        
        for (Entry<Class<?>, Class<? extends JsonSerializer<?>>> entry: configuration.getJsonSerializerClasses()) {
            builder.registerTypeAdapter(entry.getKey(), injector.getInstance(entry.getValue()));
        }

        for (Entry<Class<?>, Class<? extends JsonDeserializer<?>>> entry: configuration.getJsonDeserializerClasses()) {
            builder.registerTypeAdapter(entry.getKey(), injector.getInstance(entry.getValue()));
        }        
        
        return builder.create();
    }
    
    @SuppressWarnings("unchecked")
    private InitializerProvider configureInitializers() {
        InitializerProvider provider = new InitializerProvider();

        List<Class<?>> classes = ClassScanner.getClasses(configuration.getViewComponentRootPackages());

        for (Class<?> cl : classes) {
            if (Component.class.isAssignableFrom(cl)
                    && cl.getAnnotation(View.class) != null) {
                provider.addInitializer((Class<? extends Component>) cl);
            }
        }

        return provider;
    }
}