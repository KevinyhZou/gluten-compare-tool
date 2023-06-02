package io.glutenproject.sql.compare;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SessionCache {

    private Map<String, String> glutenSessionIds;
    private Map<String, String> sparkSessionIds;

    public SessionCache() {
        this.glutenSessionIds = new HashMap<>();
        this.sparkSessionIds = new HashMap<>();
    }

    public void putGlutenSessionId(String user, String sessionId) {
        this.glutenSessionIds.put(user, sessionId);
    }

    public String getGlutenSessionId(String user) {
        return this.glutenSessionIds.get(user);
    }

    public Map<String, String> getGlutenSessionIds() {
        return glutenSessionIds;
    }

    public void putSparkSessionId(String user, String sessionId) {
        this.sparkSessionIds.put(user, sessionId);
    }

    public String getSparkSessionId(String user) {
        return this.sparkSessionIds.get(user);
    }

    public Map<String, String> getSparkSessionIds() {
        return sparkSessionIds;
    }
}
