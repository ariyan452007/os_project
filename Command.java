import java.util.ArrayList;
import java.util.List;

/**
 * OS CONCEPT: Process Image Descriptor
 * This acts like the internal 'struct command' used in a C-based shell.
 * It contains the parsed argument vector (argv) and file descriptor routing instructions 
 * before the actual fork/exec takes place.
 */
public class Command {
    public List<String> args = new ArrayList<>();
    
    // File Descriptors mapping
    public String stdoutRedirect = null;
    public boolean stdoutAppend = false;
    
    public String stderrRedirect = null;
    public boolean stderrAppend = false;
    
    // Process Grouping
    public boolean isBackground = false;
}
