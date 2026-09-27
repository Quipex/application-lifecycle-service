package com.appmanagement.infrastructure.application;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@TestConfiguration
class ProjectionResultSetInspector {

    @Bean
    static ResultColumnCapture resultColumnCapture() {
        return new ResultColumnCapture();
    }
}

final class ResultColumnCapture implements BeanPostProcessor {

    private final List<Integer> resultColumnCounts = new CopyOnWriteArrayList<>();

    void clear() {
        resultColumnCounts.clear();
    }

    List<Integer> resultColumnCounts() {
        return List.copyOf(resultColumnCounts);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource && !Proxy.isProxyClass(bean.getClass())) {
            return proxy(DataSource.class, (target, method, args) -> {
                var result = invoke(dataSource, method, args);
                if (result instanceof Connection connection) {
                    return inspectConnection(connection);
                }
                return result;
            });
        }
        return bean;
    }

    private Connection inspectConnection(Connection connection) {
        return (Connection) proxy(Connection.class, (target, method, args) -> {
            var result = invoke(connection, method, args);
            if (result instanceof Statement statement
                    && (method.getName().equals("createStatement")
                    || method.getName().equals("prepareStatement")
                    || method.getName().equals("prepareCall"))) {
                return inspectStatement(statement);
            }
            return result;
        });
    }

    private Statement inspectStatement(Statement statement) {
        var statementType = statement instanceof CallableStatement
                ? CallableStatement.class
                : statement instanceof PreparedStatement ? PreparedStatement.class : Statement.class;
        return (Statement) proxy(statementType, (target, method, args) -> {
            var result = invoke(statement, method, args);
            if (result instanceof ResultSet resultSet
                    && (method.getName().equals("executeQuery") || method.getName().equals("getResultSet"))) {
                resultColumnCounts.add(resultSet.getMetaData().getColumnCount());
            }
            return result;
        });
    }

    private static Object proxy(Class<?> type, InvocationHandler handler) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }
}
