package com.A509.Service;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.sql.Timestamp;
import java.net.URI;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class AnalyticsService {
    private final JdbcTemplate jdbc;
    private final AtomicLong lastCleanup = new AtomicLong();
    @Value("${app.analytics.proxy-secret:}") private String proxySecret;
    @Value("${app.analytics.retention-days:90}") private int retentionDays;
    public AnalyticsService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public record Visit(String eventId, String visitorId, String sessionId, String path, String referrer) {}
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    public void record(Visit visit, HttpServletRequest request) {
        if (visit==null || !uuid(visit.eventId) || !uuid(visit.visitorId) || !uuid(visit.sessionId)) throw invalid();
        if ("1".equals(request.getHeader("DNT"))) return;
        String path=visit.path == null ? "" : visit.path.split("[?#]",2)[0];
        if (!path.startsWith("/") || path.startsWith("//") || path.length()>500 || path.chars().anyMatch(c -> c<32)) throw invalid();
        if (path.matches("(?i)^/(admin|reset-password|profile|login)(/.*)?$")) return;
        String ua=Optional.ofNullable(request.getHeader("User-Agent")).orElse("").toLowerCase(Locale.ROOT);
        if (ua.matches(".*(bot|crawler|spider|headless).*")) return;
        String device=ua.contains("ipad") || ua.contains("tablet") || ua.contains("android") && !ua.contains("mobile") ? "tablet"
            : ua.contains("mobile") || ua.contains("iphone") ? "phone" : "desktop";
        String referrer="direct";
        try {
            if (visit.referrer != null && visit.referrer.length()<=2000) {
                URI ref=URI.create(visit.referrer);
                if (Set.of("http","https").contains(Optional.ofNullable(ref.getScheme()).orElse("")) && ref.getHost()!=null)
                    referrer=ref.getHost().toLowerCase(Locale.ROOT);
            }
        } catch (IllegalArgumentException ignored) { }
        if (referrer.length()>253) referrer="direct";
        String country="ZZ",region="Unknown";
        // Only a configured proxy that overwrites these headers and injects a secret can supply location.
        String supplied=request.getHeader("X-A509-Proxy-Secret");
        if (proxySecret!=null && !proxySecret.isBlank() && supplied!=null && MessageDigest.isEqual(proxySecret.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8))) {
            String candidate=request.getHeader("X-A509-Country");
            if (candidate!=null && candidate.matches("[A-Z]{2}") && Arrays.asList(Locale.getISOCountries()).contains(candidate)) country=candidate;
            String area=request.getHeader("X-A509-Region");
            if (!country.equals("ZZ") && area!=null && area.length()<=100 && area.matches("[\\p{L}\\p{N} ._-]+")) region=area;
        }
        LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        try { jdbc.update("insert into page_views(event_id,viewed_at,visitor_hash,session_hash,path,referrer,device,country,region) values(?,?,?,?,?,?,?,?,?)",
            visit.eventId,Timestamp.valueOf(now),hash(visit.visitorId),hash(visit.sessionId),path,referrer,device,country,region);
        } catch (DuplicateKeyException ignored) { /* retry / StrictMode must not count twice */ }
        long millis=System.currentTimeMillis(),old=lastCleanup.get();
        if (millis-old>3600000 && lastCleanup.compareAndSet(old,millis))
            jdbc.update("delete from page_views where viewed_at < ?",Timestamp.valueOf(now.minusDays(Math.max(7,retentionDays))));
    }
    private boolean uuid(String value) { return value!=null && value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"); }
    private ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid analytics event"); }
    public Map<String,Object> overview(int requestedDays) {
        int days=Set.of(7,30,90).contains(requestedDays)?requestedDays:30;
        LocalDate today=LocalDate.now(ZoneOffset.UTC),start=today.minusDays(days-1);
        Timestamp since=Timestamp.valueOf(start.atStartOfDay());
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("days",days);result.put("timezone","UTC");result.put("retentionDays",Math.max(7,retentionDays));
        result.put("users",scalar("select count(*) from users"));
        result.put("activeUsers",scalar("select count(*) from users where status=true"));
        result.put("publishedArticles",scalar("select count(*) from articles where status='PUBLISHED' and deleted_at is null"));
        result.put("uniforms",scalar("select count(*) from uniforms"));
        result.put("pageviews",scalar("select count(*) from page_views where viewed_at>=?",since));
        result.put("visitors",scalar("select count(distinct visitor_hash) from page_views where viewed_at>=?",since));
        result.put("sessions",scalar("select count(distinct session_hash) from page_views where viewed_at>=?",since));
        result.put("newUsers",scalar("select count(*) from users where created_at>=?",since));
        List<Map<String,Object>> daily=new ArrayList<>();
        var rows=jdbc.queryForList("select cast(viewed_at as date) as bucket_date,count(*) as views,count(distinct session_hash) as sessions from page_views where viewed_at>=? group by cast(viewed_at as date)",since);
        var accounts=jdbc.queryForList("select cast(created_at as date) as bucket_date,count(*) as total from users where created_at>=? group by cast(created_at as date)",since);
        for (LocalDate date=start;!date.isAfter(today);date=date.plusDays(1)) {
            String day=date.toString();Map<String,Object> point=new LinkedHashMap<>();point.put("day",day);point.put("views",0);point.put("sessions",0);point.put("users",0);
            rows.stream().filter(r->day.equals(String.valueOf(r.get("bucket_date")))).findFirst().ifPresent(r->{point.put("views",r.get("views"));point.put("sessions",r.get("sessions"));});
            accounts.stream().filter(r->day.equals(String.valueOf(r.get("bucket_date")))).findFirst().ifPresent(r->point.put("users",r.get("total")));
            daily.add(point);
        }
        result.put("daily",daily);
        for(String dimension:List.of("device","referrer","country","region","path")) {
            result.put(dimension,jdbc.queryForList("select "+dimension+" as label,count(*) as total from page_views where viewed_at>=? group by "+dimension+" order by count(*) desc,"+dimension+" limit 10",since).stream().map(row->Map.of("label",row.get("label"),"total",row.get("total"))).toList());
        }
        result.put("locationConfigured",proxySecret!=null&&!proxySecret.isBlank());
        return result;
    }
    private long scalar(String sql,Object...args) { Long n=jdbc.queryForObject(sql,Long.class,args);return n==null?0:n; }
}
