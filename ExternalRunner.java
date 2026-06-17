import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * OS CONCEPT: Process Creation (fork/exec) and I/O Redirection (dup2)
 * Wraps ProcessBuilder, translating our Command objects into actual OS-level child processes.
 * Configures the file descriptor mapping directly at the OS level using ProcessBuilder.Redirect, 
 * simulating the dup2() syscall.
 */
public class ExternalRunner {
    public static int execute(Command cmd, InputStream in, PrintStream out, PrintStream err) {
        String name = cmd.args.get(0);
        String path = Builtins.findInPath(name);
        if (path == null) {
            err.printf("%s: command not found\n", name);
            return 127;
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd.args);
            pb.directory(new File(Main.cwd));
            
            // Map standard outputs. Equivalent to open() -> dup2(fd, STDOUT_FILENO)
            if (cmd.stdoutRedirect != null) {
                pb.redirectOutput(cmd.stdoutAppend ? 
                    ProcessBuilder.Redirect.appendTo(new File(cmd.stdoutRedirect)) : 
                    ProcessBuilder.Redirect.to(new File(cmd.stdoutRedirect)));
            } else if (out == System.out) {
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            } else {
                pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            }
            
            if (cmd.stderrRedirect != null) {
                pb.redirectError(cmd.stderrAppend ? 
                    ProcessBuilder.Redirect.appendTo(new File(cmd.stderrRedirect)) : 
                    ProcessBuilder.Redirect.to(new File(cmd.stderrRedirect)));
            } else if (err == System.err) {
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            } else {
                pb.redirectError(ProcessBuilder.Redirect.PIPE);
            }
            
            if (in == System.in) {
                pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            } else {
                pb.redirectInput(ProcessBuilder.Redirect.PIPE);
            }

            Process p = pb.start(); // This simulates the OS fork() & execvp()
            
            if (cmd.isBackground) {
                JobTable.addJob(p, String.join(" ", cmd.args));
                return 0; // Return immediately, do not wait.
            } else {
                // If we are artificially piping streams internally to Java threads
                if (cmd.stdoutRedirect == null && out != System.out) {
                    transferStream(p.getInputStream(), out);
                }
                if (cmd.stderrRedirect == null && err != System.err) {
                    transferStream(p.getErrorStream(), err);
                }
                if (in != System.in) {
                    transferStream(in, p.getOutputStream());
                    p.getOutputStream().close();
                }
                
                return p.waitFor(); // Simulates waitpid()
            }
        } catch (Exception e) {
            err.println(e.getMessage());
            return 1;
        }
    }
    
    private static void transferStream(InputStream is, OutputStream os) {
        new Thread(() -> {
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) {
                    os.write(buf, 0, n);
                    os.flush();
                }
            } catch (Exception ignored) {}
        }).start();
    }
}
