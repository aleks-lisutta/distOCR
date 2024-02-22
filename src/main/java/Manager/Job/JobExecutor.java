package Manager.Job;

public interface JobExecutor {
    String createJobExecutor();
    void deleteJobExecutor(String id);
    void createWorkers();
    void deleteJobExecutors();
}
