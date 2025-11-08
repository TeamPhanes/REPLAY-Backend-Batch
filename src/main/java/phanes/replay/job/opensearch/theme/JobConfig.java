package phanes.replay.job.opensearch.theme;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import phanes.replay.job.opensearch.theme.domain.ThemeDoc;

@Configuration
@RequiredArgsConstructor
public class JobConfig {

    private final ThemeReader themeReader;
    private final OpenSearchWriter openSearchWriter;
    private final JobLauncher jobLauncher;

    @Bean
    public Job themeIndexJob(JobRepository jobRepository, Step themeIndexStep) {
        return new JobBuilder("themeIndexJob", jobRepository)
                .start(themeIndexStep)
                .build();
    }

    @Bean
    public Step themeIndexStep(JobRepository jobRepository, PlatformTransactionManager tx) {
        return new StepBuilder("themeIndexStep", jobRepository)
                .<ThemeDoc, ThemeDoc>chunk(1000, tx)
                .reader(themeReader)
                .writer(openSearchWriter)
                .faultTolerant()
                .build();
    }

    @Bean
    public CommandLineRunner jobRunner(Job themeIndexJob) {
        return args -> {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(themeIndexJob, jobParameters);
        };
    }
}