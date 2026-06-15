# Java Unix Shell (CodeCrafters)

A fully functional, Unix-like shell implemented entirely in Java. This project was built to pass the CodeCrafters Shell Challenge, completing all 31 mandatory stages across 6 advanced OS modules.

## Features Implemented
1. **Base REPL:** Parses input, handles builtins (`exit`, `echo`, `type`), and executes external commands via `PATH` resolution.
2. **Navigation:** Supports absolute/relative paths and `$HOME` resolution using `cd` and `pwd`.
3. **Robust Quoting:** State-machine based lexer correctly processes single quotes, double quotes, and arbitrary backslash escaping.
4. **I/O Redirection:** Direct OS-level file descriptor mapping for stdout/stderr (`>`, `>>`, `2>`, `2>>`) using `ProcessBuilder.Redirect` to ensure zero-overhead file writing.
5. **Background Jobs:** Handles background process spawning (`&`), zombie process reaping via `isAlive()` polling (simulating `waitpid` with `WNOHANG`), and a `jobs` list.
6. **Pipelines:** Multi-stage pipeline architecture (`cmd1 | cmd2`) mapping `stdout` to `stdin` across concurrent processes using Thread-based data pumps and OS pipes.

## Architecture & OS Concepts
- **`Main.java`**: The core Read-Eval-Print-Loop (REPL) and process lifecycle manager.
- **`Parser.java`**: Lexical tokenization of raw input, strictly adhering to Bash quoting rules.
- **`Command.java`**: The parsed process image struct.
- **`Executor.java`**: Dispatcher separating builtin execution from external process forks.
- **`ExternalRunner.java`**: The core `fork()` / `exec()` equivalent wrapper.
- **`Builtins.java`**: Internal OS commands (cd, pwd, etc.) executing within the parent process.
- **`JobTable.java`**: Process control block managing background process tracking and zombie reaping.
- **`Pipeline.java`**: IPC mechanism wiring multiple commands together via pipes.

## How to Run

1. **Compile the project:**
   ```bash
   javac *.java
   ```

2. **Run the shell:**
   ```bash
   java Main
   ```

Once inside the shell (`$ ` prompt), you can run standard Unix commands, navigate directories, pipe outputs, and run background processes!
