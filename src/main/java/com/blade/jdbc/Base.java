package com.blade.jdbc;

import com.blade.jdbc.page.PageRow;
import lombok.extern.slf4j.Slf4j;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import javax.sql.DataSource;
import java.util.function.Supplier;

@Slf4j
public final class Base {

    public static final ThreadLocal<PageRow>    pageLocal             = new ThreadLocal<>();
    public static final ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();

    public static Sql2o sql2o;

    public static Sql2o open(String url, String user, String password) {
        sql2o = new Sql2o(url, user, password);
        return sql2o;
    }

    public static Sql2o open(DataSource dataSource) {
        sql2o = new Sql2o(dataSource);
        return sql2o;
    }

    /**
     * 原子提交
     *
     * @param supplier
     * @param <T>
     */
    public static <T> T atomic(Supplier<T> supplier) {
        T result = null;
        try {
            connectionThreadLocal.remove();
            connectionThreadLocal.set(sql2o.beginTransaction());
            try (Connection con = connectionThreadLocal.get()) {
                result = supplier.get();
                con.commit();
            }
        } catch (RuntimeException e) {
            log.info("Transaction rollback");
            connectionThreadLocal.get().rollback();
            throw e;
        } finally {
            connectionThreadLocal.remove();
            return result;
        }
    }

}