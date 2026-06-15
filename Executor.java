import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * OS CONCEPT: Command Dispatcher
 * The OS shell must decide whether a command executes within its own process space 
 * (builtins, like changing cwd) or requires spawning a new child process (external commands).
 * It also sets up standard stream redirection via custom PrintStreams (for builtins) 
 * or passes instructions to ExternalRunner.
 */
public class Executor {
    public static int execute(Command cmd, InputStream in, PrintStream out, PrintStream err) {
        if (cmd.args.isEmpty()) return 0;
        String name = cmd.args.get(0);
        
        if (Builtins.isBuiltin(name)) {
            PrintStream actualOut = out;
            PrintStream actualErr = err;
            try {
                // Builtins manually hijack Java's output streams 
                // to simulate file descriptor redirection.
                if (cmd.stdoutRedirect != null) {
                    actualOut = new PrintStream(new FileOutputStream(cmd.stdoutRedirect, cmd.stdoutAppend));
                }
                if (cmd.stderrRedirect != null) {
                    actualErr = new PrintStream(new FileOutputStream(cmd.stderrRedirect, cmd.stderrAppend));
                }
            } catch (Exception e) {
                err.println(e.getMessage());
                return 1;
            }
            
            int exitCode = Builtins.execute(cmd, actualOut, actualErr);
            
            // Clean up custom file streams so we don't leak resources.
            if (actualOut != out) actualOut.close();
            if (actualErr != err) actualErr.close();
            
            return exitCode;
        } else {
            return ExternalRunner.execute(cmd, in, out, err);
        }
    }
}
