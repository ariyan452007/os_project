import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

/**
 * OS CONCEPT: Shell REPL (Read-Eval-Print Loop)
 * The main loop represents the core of a shell's lifecycle. 
 * It continuously reads user input, parses it into instructions, and evaluates them.
 * It also manages process lifecycle by "reaping" zombie processes (background jobs that have finished)
 * before presenting the next prompt.
 */
public class Main {
    // Tracks the current working directory, analogous to the process's CWD in the OS.
    public static String cwd = System.getProperty("user.dir");
    public static java.util.List<String> historyList = new java.util.ArrayList<>();
    
    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        while (true) {
            // Reap completed background jobs to prevent zombie processes.
            JobTable.reapJobs(); 
            
            // Print the prompt and force output to flush to the terminal.
            System.out.print("$ ");
            System.out.flush();
            
            String line = reader.readLine();
            if (line == null) break; // EOF (Ctrl+D)
            if (line.trim().isEmpty()) continue;
            historyList.add(line);
            
            List<Command> pipeline = Parser.parse(line);
            if (pipeline.isEmpty()) continue;
            
            if (pipeline.size() == 1) {
                Command cmd = pipeline.get(0);
                // Background execution vs Foreground blocking execution
                if (cmd.isBackground) {
                    Executor.execute(cmd, System.in, System.out, System.err);
                } else {
                    Executor.execute(cmd, System.in, System.out, System.err);
                }
            } else {
                Pipeline.execute(pipeline);
            }
        }
    }
}
