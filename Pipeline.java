import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * OS CONCEPT: Inter-Process Communication (IPC) via Pipes
 * Wires the standard output of one command to the standard input of the next.
 * In Java, we do this by manipulating InputStream/OutputStream and spinning up
 * async
 * data pumps, or using OS pipe buffers through ProcessBuilder.Redirect.PIPE.
 */
public class Pipeline {
    public static void execute(List<Command> commands) {
        boolean hasBuiltin = false;
        for (Command cmd : commands) {
            if (Builtins.isBuiltin(cmd.args.get(0))) {
                hasBuiltin = true;
                break;
            }
        }

        if (!hasBuiltin) {
            List<ProcessBuilder> builders = new ArrayList<>();
            for (int i = 0; i < commands.size(); i++) {
                Command cmd = commands.get(i);
                ProcessBuilder pb = new ProcessBuilder(cmd.args);
                pb.directory(new File(Main.cwd));
                
                if (i == 0) {
                    pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
                }
                if (i == commands.size() - 1) {
                    if (cmd.stdoutRedirect != null) {
                        pb.redirectOutput(cmd.stdoutAppend ? 
                            ProcessBuilder.Redirect.appendTo(new File(cmd.stdoutRedirect)) : 
                            ProcessBuilder.Redirect.to(new File(cmd.stdoutRedirect)));
                    } else {
                        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    }
                    if (cmd.stderrRedirect != null) {
                        pb.redirectError(cmd.stderrAppend ? 
                            ProcessBuilder.Redirect.appendTo(new File(cmd.stderrRedirect)) : 
                            ProcessBuilder.Redirect.to(new File(cmd.stderrRedirect)));
                    } else {
                        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                    }
                } else {
                    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                }
                builders.add(pb);
            }
            try {
                List<Process> procs = ProcessBuilder.startPipeline(builders);
                for (Process p : procs) {
                    p.waitFor();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        List<Process> processes = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        InputStream previousOut = System.in;

        try {
            for (int i = 0; i < commands.size(); i++) {
                Command cmd = commands.get(i);
                boolean isFirst = (i == 0);
                boolean isLast = (i == commands.size() - 1);

                if (Builtins.isBuiltin(cmd.args.get(0))) {
                    PipedInputStream pipeIn = null;
                    PipedOutputStream pipeOut = null;

                    if (!isLast) {
                        pipeIn = new PipedInputStream();
                        pipeOut = new PipedOutputStream(pipeIn);
                    }

                    final InputStream inStream = previousOut;
                    final PrintStream outStream = isLast ? System.out : new PrintStream(pipeOut);

                    Thread t = new Thread(() -> {
                        Executor.execute(cmd, inStream, outStream, System.err);
                        if (!isLast)
                            outStream.close();
                    });
                    t.start();
                    threads.add(t);

                    previousOut = pipeIn;
                } else {
                    String path = Builtins.findInPath(cmd.args.get(0));
                    if (path == null) {
                        System.err.printf("%s: command not found\n", cmd.args.get(0));
                        break;
                    }

                    ProcessBuilder pb = new ProcessBuilder(cmd.args);
                    pb.directory(new File(Main.cwd));

                    if (isFirst)
                        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
                    else
                        pb.redirectInput(ProcessBuilder.Redirect.PIPE);

                    if (isLast)
                        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    else
                        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);

                    pb.redirectError(ProcessBuilder.Redirect.INHERIT);

                    Process p = pb.start();
                    processes.add(p);

                    if (!isFirst) {
                        final InputStream inStream = previousOut;
                        final OutputStream outStream = p.getOutputStream();
                        Thread t = new Thread(() -> {
                            try {
                                byte[] buf = new byte[8192];
                                int n;
                                while ((n = inStream.read(buf)) != -1) {
                                    outStream.write(buf, 0, n);
                                    outStream.flush();
                                }
                                outStream.close();
                            } catch (Exception e) {
                            }
                        });
                        t.start();
                        threads.add(t);
                    }

                    previousOut = p.getInputStream();
                }
            }

            // Wait for all stages to synchronize
            for (Process p : processes)
                p.waitFor();
            for (Thread t : threads)
                t.join();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
