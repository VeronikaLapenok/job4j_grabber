package ru.job4j.grabber;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class Grabber implements Grab {
    private final Properties config = new Properties();

    public Store store() {
        return new PsqlStore(config);
    }

    public Scheduler scheduler() throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        return scheduler;
    }

    public void loadConfig() throws IOException {
        try (InputStream in = Grabber.class.getClassLoader().getResourceAsStream("post.properties")) {
            config.load(in);
        }
    }

    @Override
    public void init(Parse parse, Store store, Scheduler scheduler) throws SchedulerException {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("store", store);
        dataMap.put("parse", parse);
        JobDetail job = JobBuilder.newJob(GrabJob.class)
                .usingJobData(dataMap)
                .build();
        SimpleScheduleBuilder times = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(Integer.parseInt(config.getProperty("time")))
                .repeatForever();
        Trigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(times)
                .build();
        scheduler.scheduleJob(job, trigger);
    }

    public static class GrabJob implements Job {
        private static final String LINK = "https://career.habr.com/vacancies/java_developer?page=";
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            JobDataMap map = jobExecutionContext.getJobDetail().getJobDataMap();
            Store store = (Store) map.get("store");
            Parse parse = (Parse) map.get("parse");
            try {
                List<Post> posts = parse.list(LINK);
                for (Post post : posts) {
                    store.save(post);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Grabber grabber = new Grabber();
        grabber.loadConfig();
        Scheduler scheduler = grabber.scheduler();
        Store store = grabber.store();
        grabber.init(new HabrCareerParse(new HabrCareerDateTimeParser()), store, scheduler);
    }
}
