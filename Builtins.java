import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * OS CONCEPT: Builtin Commands
 * System calls like chdir() (cd) or exit() must run within the parent shell process. 
 * If they ran in a child, the environment changes would be lost once the child died.
 */
public class Builtins {
    private static final Set<String> BUILTINS = new HashSet<>(Arrays.asList("exit", "echo", "type", "pwd", "cd", "jobs"));
    
    public static boolean isBuiltin(String name) {
        return BUILTINS.contains(name);
    }
    
    public static String findInPath(String cmd) {
        File f = new File(cmd);
        if (f.exists() && f.canExecute() && !f.isDirectory()) {
            return f.getAbsolutePath();
        }
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return null;
        for (String dir : pathEnv.split(":")) {
            File file = new File(dir, cmd);
            if (file.exists() && file.canExecute() && !file.isDirectory()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }
    
    public static int execute(Command cmd, PrintStream out, PrintStream err) {
        String name = cmd.args.get(0);
        List<String> args = cmd.args.subList(1, cmd.args.size());
        
        switch (name) {
            case "exit":
                int code = 0;
                if (!args.isEmpty()) {
                    try { code = Integer.parseInt(args.get(0)); } catch (Exception ignored) {}
                }
                System.exit(code);
                return code;
            case "echo":
                out.println(String.join(" ", args));
                return 0;
            case "type":
                if (args.isEmpty()) return 0;
                String target = args.get(0);
                if (isBuiltin(target)) {
                    out.printf("%s is a shell builtin\n", target);
                } else {
                    String path = findInPath(target);
                    if (path != null) {
                        out.printf("%s is %s\n", target, path);
                    } else {
                        err.printf("%s: not found\n", target);
                    }
                }
                return 0;
            case "pwd":
                out.println(Main.cwd);
                return 0;
            case "cd":
                if (args.isEmpty()) return 0;
                String dir = args.get(0);
                if (dir.equals("~")) dir = System.getenv("HOME");
                File newDir = new File(dir);
                if (!newDir.isAbsolute()) newDir = new File(Main.cwd, dir);
                try {
                    if (newDir.exists() && newDir.isDirectory()) {
                        Main.cwd = newDir.getCanonicalPath();
                    } else {
                        err.printf("cd: %s: No such file or directory\n", args.get(0));
                    }
                } catch (Exception e) {
                    err.println(e.getMessage());
                }
                return 0;
            case "jobs":
                JobTable.listJobs(out);
                return 0;
        }
        return 1;
    }
}
