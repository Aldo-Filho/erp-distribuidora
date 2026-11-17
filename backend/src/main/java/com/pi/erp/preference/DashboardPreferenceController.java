package com.pi.erp.preference;
import com.pi.erp.auth.CurrentUser;
import com.pi.erp.dashboard.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import static com.pi.erp.preference.DashboardPreferenceDTO.*;
@RestController @RequestMapping("/preferences/current/dashboard")
public class DashboardPreferenceController {
    @Autowired private DashboardPreferenceRepository repository;
    @Autowired private DashboardCatalog catalog;
    @Autowired private DashboardPeriodResolver periods;
    @Autowired private ObjectMapper mapper;
    private static final Set<String> PRESETS=Set.of("DAY","WEEK","FORTNIGHT","MONTH","YEAR","CUSTOM");
    private Filter defaultFilter() { return new Filter("MONTH",periods.today(),null,null,false); }

    private static void appendRows(List<Widget> layout, List<DashboardCatalog.Definition> blocks, int columns) {
        int y = layout.stream().mapToInt(w -> w.y() + w.h()).max().orElse(0);
        for (int start = 0; start < blocks.size(); start += columns) {
            var row = blocks.subList(start, Math.min(start + columns, blocks.size()));
            int width = 12 / row.size();
            for (int i = 0; i < row.size(); i++) {
                var block = row.get(i);
                layout.add(new Widget(block.id(), true, i * width, y, width, block.minH()));
            }
            y += row.stream().mapToInt(DashboardCatalog.Definition::minH).max().orElseThrow();
        }
    }

    private List<Widget> reconcile(List<Widget> saved) {
        var allowed=catalog.allowed(); List<Widget> result=new ArrayList<>();
        Set<String> seen=new HashSet<>();
        for(var w:saved) if(allowed.stream().anyMatch(d->d.id().equals(w.id()))) {result.add(w);seen.add(w.id());}

        var missing = allowed.stream().filter(d -> !seen.contains(d.id())).toList();
        appendRows(result, missing.stream().filter(d -> d.kind().equals("STAT")).toList(), 4);
        appendRows(result, missing.stream().filter(d -> d.id().equals("sales.revenue")).toList(), 1);
        appendRows(result, missing.stream().filter(d -> d.kind().equals("LINE") && !d.id().equals("sales.revenue")).toList(), 2);

        var charts = missing.stream().filter(d -> d.kind().equals("BAR")).toList();
        for (String profile : charts.stream().map(DashboardCatalog.Definition::profile).distinct().toList()) {
            appendRows(result, charts.stream().filter(d -> d.profile().equals(profile)).toList(), 2);
        }
        appendRows(result, missing.stream().filter(d -> d.kind().equals("RANKING")).toList(), 2);
        appendRows(result, missing.stream().filter(d -> d.kind().equals("AGENDA")).toList(), 2);
        return result;
    }
    private DashboardPreferenceDTO defaults(long revision) {
        return new DashboardPreferenceDTO(1, revision, reconcile(List.of()), defaultFilter());
    }
    private DashboardPreferenceDTO read() {
        var stored=repository.find(CurrentUser.requireId());
        if (stored == null) return defaults(0);
        var dto=mapper.readValue(stored.payload(),DashboardPreferenceDTO.class);
        return new DashboardPreferenceDTO(1,stored.revision(),reconcile(dto.widgets()),dto.filter());
    }
    @GetMapping public ResponseEntity<DashboardPreferenceDTO> get() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(read());
    }
    @GetMapping(params = "defaults=true")
    public ResponseEntity<DashboardPreferenceDTO> getDefaults() {
        var stored = repository.find(CurrentUser.requireId());
        long revision = stored == null ? 0 : stored.revision();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(defaults(revision));
    }
    private void keys(JsonNode node,Set<String> allowed) {
        if(node==null||!node.isObject()) throw new IllegalArgumentException("Objeto inválido.");
        for(String key:node.propertyNames()) if(!allowed.contains(key)) throw new IllegalArgumentException("Campo de preferência desconhecido.");
    }
    private void integer(JsonNode node,String key) {
        if(!node.has(key)||!node.get(key).isIntegralNumber()||!node.get(key).canConvertToInt()) throw new IllegalArgumentException("Coordenadas devem ser inteiras.");
    }
    @PutMapping @Transactional public ResponseEntity<DashboardPreferenceDTO> put(@RequestBody String body) {
        CurrentUser.requireId();
        if(body.getBytes(StandardCharsets.UTF_8).length>16384) throw new IllegalArgumentException("Preferência limitada a 16 KiB.");
        JsonNode root=mapper.readTree(body);
        keys(root,Set.of("schemaVersion","revision","widgets","filter"));
        integer(root,"schemaVersion");
        if(!root.has("revision")||!root.get("revision").isIntegralNumber()||!root.get("revision").canConvertToLong()||root.get("revision").asLong()<0) throw new IllegalArgumentException("Revisão inválida.");
        if(!root.has("widgets")||!root.get("widgets").isArray()||root.get("widgets").size()>catalog.definitions.size()) throw new IllegalArgumentException("Blocos inválidos.");
        for(var w:root.get("widgets")) {
            keys(w,Set.of("id","visible","x","y","w","h"));
            for(String key:List.of("x","y","w","h")) integer(w,key);
            if(!w.has("visible")||!w.get("visible").isBoolean()||!w.has("id")||!w.get("id").isTextual()) throw new IllegalArgumentException("Bloco inválido.");
        }
        if(!root.has("filter")) throw new IllegalArgumentException("Filtro obrigatório.");
        keys(root.get("filter"),Set.of("preset","referenceDate","from","to","compare"));
        if(!root.get("filter").has("compare")||!root.get("filter").get("compare").isBoolean()) throw new IllegalArgumentException("Comparação inválida.");
        var dto=mapper.treeToValue(root,DashboardPreferenceDTO.class);
        if(dto.schemaVersion()!=1) throw new IllegalArgumentException("Versão de esquema inválida.");
        Set<String> ids=new HashSet<>();
        for(var w:dto.widgets()) {
            if(catalog.definitions.stream().noneMatch(d->d.id().equals(w.id()))) throw new IllegalArgumentException("Bloco desconhecido.");
            var d=catalog.require(w.id());
            if(!ids.add(w.id())||w.x()<0||w.x()>11||w.y()<0||w.y()>100000||w.w()<d.minW()||w.w()>12||w.h()<d.minH()||w.h()>24||w.x()+w.w()>12)
                throw new IllegalArgumentException("Layout inválido.");
            if(w.visible()) for(var other:dto.widgets()) if(other!=w && other.visible() && w.x()<other.x()+other.w() && w.x()+w.w()>other.x() && w.y()<other.y()+other.h() && w.y()+w.h()>other.y())
                throw new IllegalArgumentException("Blocos sobrepostos.");
        }
        var f=dto.filter();
        if(f==null||f.preset()==null||!PRESETS.contains(f.preset())||f.referenceDate()==null||f.referenceDate().isAfter(periods.today())) throw new IllegalArgumentException("Filtro inválido.");
        if(f.preset().equals("CUSTOM")) { if(f.from()==null||f.to()==null) throw new IllegalArgumentException("Intervalo obrigatório.");periods.resolve(f.from(),f.to(),f.compare()); }
        else if(f.from()!=null||f.to()!=null) throw new IllegalArgumentException("Datas personalizadas exigem preset CUSTOM.");
        repository.save(CurrentUser.requireId(),mapper.writeValueAsString(dto),dto.revision());
        return get();
    }
    @ExceptionHandler(tools.jackson.core.JacksonException.class)
    public ResponseEntity<Map<String,String>> malformed() {
        return ResponseEntity.badRequest().body(Map.of("message","Preferência inválida."));
    }
    @DeleteMapping @Transactional public ResponseEntity<DashboardPreferenceDTO> delete(@RequestParam long revision) {
        // Logical deletion keeps a monotonic revision, preventing stale tabs from overwriting a reset (ABA).
        var old=read();
        if(revision!=old.revision()) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT);
        repository.save(CurrentUser.requireId(),mapper.writeValueAsString(defaults(old.revision())),old.revision());
        return get();
    }
}
