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
    
    public static void addJob(Process p, String cmdStr) {
        Job j = new Job();
        j.id = nextId++;
        j.pid = p.pid();
        j.commandStr = cmdStr;
        j.process = p;
        jobs.add(j);
    }
    
    public static void reapJobs() {
        Iterator<Job> it = jobs.iterator();
        while(it.hasNext()) {
            Job j = it.next();
            // Checking process status without blocking (simulates waitpid(..., WNOHANG))
            if (!j.process.isAlive()) {
                System.out.printf("[%d]+ Done %s\n", j.id, j.commandStr);
                it.remove();
            }
        }
    }
    
    public static void listJobs(PrintStream out) {
        for (Job j : jobs) {
            out.printf("[%d] %d Running %s\n", j.id, j.pid, j.commandStr);
        }
    }
}
