package phanes.replay.job.opensearch.theme;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.stereotype.Component;
import phanes.replay.job.opensearch.theme.domain.CafeDoc;
import phanes.replay.job.opensearch.theme.domain.SpotDoc;
import phanes.replay.job.opensearch.theme.domain.ThemeDoc;
import phanes.replay.job.opensearch.theme.domain.enums.Level;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThemeReader extends AbstractPagingItemReader<ThemeDoc> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EntityManager em;
    // TODO add location properties => 'location', JSON_OBJECT('lat', s.lat, 'lon', s.lng)
    private static final String QUERY = """
            SELECT t.id,
                   t.title,
                   t.playtime,
                   t.level,
                   JSON_OBJECT(
                           'name', s.name,
                           'state', s.state,
                           'city', s.city,
                           'address', s.address
                   )                                                                  AS spot,
                   JSON_OBJECT('name', c.name)                                        AS cafe,
                   COALESCE(JSON_ARRAYAGG(g.name), JSON_ARRAY()) AS genres,
                   t.created_at
            FROM theme t
                     JOIN spot s ON s.id = t.spot_id
                     JOIN cafe c ON c.id = s.cafe_id
                     LEFT JOIN genre g ON g.theme_id = t.id
            GROUP BY t.id
            LIMIT :limit
            OFFSET :offset
            """;

    @PostConstruct
    public void init() {
        setPageSize(500);
        setName("themePagingReader");
    }

    @Override
    protected void doReadPage() {
        if (results == null)
            results = new ArrayList<>();
        else
            results.clear();

        int offset = getPage() * getPageSize();
        Query q = em.createNativeQuery(QUERY);
        q.setParameter("offset", offset);
        q.setParameter("limit", getPageSize());
        List<Object[]> rows = q.getResultList();
        for (Object[] r : rows) {
            try {
                CafeDoc cafeDoc = objectMapper.readValue((String) r[5], CafeDoc.class);
                SpotDoc spotDoc = objectMapper.readValue((String) r[4], SpotDoc.class);
                List<String>  genreDocs = objectMapper.readValue((String) r[6], new TypeReference<>() {});
                ThemeDoc themeDoc = ThemeDoc.builder()
                        .id(((Number) r[0]).longValue())
                        .title((String) r[1])
                        .playtime((Integer) r[2])
                        .level(Level.valueOf((String) r[3]))
                        .spot(spotDoc)
                        .cafe(cafeDoc)
                        .genres(genreDocs)
                        .build();
                results.add(themeDoc);
            } catch (Exception e) {
                throw new RuntimeException("JSON parsing error", e);
            }
        }
    }
}