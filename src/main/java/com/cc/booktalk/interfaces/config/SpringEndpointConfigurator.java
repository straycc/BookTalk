package com.cc.booktalk.interfaces.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.websocket.server.ServerEndpointConfig;


/**
 * 打破 WebSocket 端点（Endpoint）与 Spring 容器之间的隔离，它作为一个“桥梁”，在 Tomcat 实例化 WebSocket 时，
 * 强制使用 Spring 的 BeanFactory 去创建这个对象，从而让 Spring 管理 bean。
 */

@Component
public class SpringEndpointConfigurator extends ServerEndpointConfig.Configurator implements ApplicationContextAware {

    private static volatile AutowireCapableBeanFactory beanFactory;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        beanFactory = applicationContext.getAutowireCapableBeanFactory(); // 拿到 Spring 的 AutowireCapableBeanFactory（自动装配bean）
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        AutowireCapableBeanFactory localBeanFactory = beanFactory;
        if (localBeanFactory == null) {
            throw new InstantiationException("Spring beanFactory is not initialized for WebSocket endpoint");
        }
        return localBeanFactory.createBean(endpointClass); // 把websocket 的实例化交给spring 工厂管理
    }
}
