import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * OS CONCEPT: Process Groups and Signal Handling (SIGCHLD)
 * Manages processes that have been sent to the background.
 * The shell needs to periodically poll for process state changes (since it can't 
 * natively catch SIGCHLD in pure Java) and clean them up from its process control block (PCB) table.
 */
public class JobTable {
    public static class Job {
        int id;
        long pid;
        String commandStr;
        Process process;
    }
    
    private static List<Job> jobs = new ArrayList<>();
    private static int nextId = 1;
    
    public static int addJob(Process p, String cmdStr) {
        Job j = new Job();
        int id = 1;
        while (true) {
            boolean taken = false;
            for (Job existing : jobs) {
                if (existing.id == id) {
                    taken = true;
                    break;
                }
            }
            if (!taken) break;
            id++;
        }
        j.id = id;
        j.pid = p.pid();
        j.commandStr = cmdStr;
        j.process = p;
        jobs.add(j);
        return j.id;
    }
    
    public static void reapJobs() {
        for (int i = 0; i < jobs.size(); i++) {
            Job j = jobs.get(i);
            // Checking process status without blocking (simulates waitpid(..., WNOHANG))
            if (!j.process.isAlive()) {
                char marker = ' ';
                if (i == jobs.size() - 1) marker = '+';
                else if (i == jobs.size() - 2) marker = '-';
                System.out.printf("[%d]%c  %-24s%s\n", j.id, marker, "Done", j.commandStr);
                jobs.remove(i);
                i--;
            }
        }
    }
    
    public static void listJobs(PrintStream out) {
        List<Job> sortedJobs = new ArrayList<>(jobs);
        sortedJobs.sort((a, b) -> Integer.compare(a.id, b.id));
        
        for (Job j : sortedJobs) {
            char marker = ' ';
            if (jobs.size() > 0 && j == jobs.get(jobs.size() - 1)) marker = '+';
            else if (jobs.size() > 1 && j == jobs.get(jobs.size() - 2)) marker = '-';
            
            out.printf("[%d]%c  %-24s%s\n", j.id, marker, "Running", j.commandStr);
        }
    }
}
