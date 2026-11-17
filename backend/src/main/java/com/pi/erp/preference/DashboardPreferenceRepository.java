package com.pi.erp.preference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Repository
public class DashboardPreferenceRepository {
    @Autowired private JdbcTemplate jdbc;
    public record Stored(String payload,long revision) {}
    public Stored find(Long userId) {
        var rows=jdbc.query("SELECT payload::text,revision FROM dashboard_preferences WHERE user_id=?",
            (rs,n)->new Stored(rs.getString(1),rs.getLong(2)),userId);
        return rows.isEmpty()?null:rows.getFirst();
    }
    public void save(Long userId,String payload,long revision) {
        int changed= revision==0 ? jdbc.update("INSERT INTO dashboard_preferences(user_id,payload) VALUES (?,CAST(? AS jsonb)) ON CONFLICT(user_id) DO NOTHING",userId,payload)
            : jdbc.update("UPDATE dashboard_preferences SET payload=CAST(? AS jsonb),revision=revision+1,updated_at=LOCALTIMESTAMP WHERE user_id=? AND revision=?",payload,userId,revision);
        if(changed!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Preferência alterada em outra aba.");
    }
}
