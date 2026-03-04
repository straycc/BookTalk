package com.cc.booktalk.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.websocket.server.ServerEndpointConfig;

@Component
public class SpringEndpointConfigurator extends ServerEndpointConfig.Configurator implements ApplicationContextAware {

    private static volatile AutowireCapableBeanFactory beanFactory;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        beanFactory = applicationContext.getAutowireCapableBeanFactory();
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        AutowireCapableBeanFactory localBeanFactory = beanFactory;
        if (localBeanFactory == null) {
            throw new InstantiationException("Spring beanFactory is not initialized for WebSocket endpoint");
        }
        return localBeanFactory.createBean(endpointClass);
    }
}
