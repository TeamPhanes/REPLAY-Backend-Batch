package phanes.replay.job.opensearch.theme.domain;

import lombok.*;
import phanes.replay.job.opensearch.theme.domain.enums.Level;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ThemeDoc {

    private Long id;
    private String title;
    private Integer playtime;
    private Level level;
    private SpotDoc spot;
    private CafeDoc cafe;
    private List<String> genres;
}