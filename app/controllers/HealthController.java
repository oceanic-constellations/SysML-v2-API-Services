package controllers;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jpa.manager.JPAManager;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.lang.management.ManagementFactory;

public class HealthController extends Controller {

    private final JPAManager jpaManager;

    @Inject
    public HealthController(JPAManager jpaManager) {
        this.jpaManager = jpaManager;
    }

    public Result check(Request request) {
        ObjectNode json = JsonNodeFactory.instance.objectNode();

        // DB接続確認
        String dbStatus;
        try {
            dbStatus = jpaManager.transact((EntityManager em) -> {
                em.createNativeQuery("SELECT 1").getSingleResult();
                return "connected";
            });
        } catch (Exception e) {
            dbStatus = "error: " + e.getMessage();
        }
        json.put("db", dbStatus);

        // JVMメモリ情報
        Runtime rt = Runtime.getRuntime();
        long heapUsed = rt.totalMemory() - rt.freeMemory();
        long heapMax = rt.maxMemory();
        json.put("heap_used_mb", heapUsed / (1024 * 1024));
        json.put("heap_max_mb", heapMax / (1024 * 1024));
        json.put("heap_usage_pct", Math.round(heapUsed * 100.0 / heapMax));

        // Uptime
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        json.put("uptime_sec", uptimeMs / 1000);

        // 全体ステータス
        json.put("status", "connected".equals(dbStatus) ? "ok" : "degraded");

        return ok(json);
    }
}
