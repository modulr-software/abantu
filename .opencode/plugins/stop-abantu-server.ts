const STOP_SCRIPT = ".opencode/skills/abantu-server/scripts/abantu-server.sh";

export const StopAbantuServerPlugin = async ({ worktree }) => {
  const stop = () => {
    try {
      Bun.spawnSync(["bash", STOP_SCRIPT, "stop"], { cwd: worktree });
    } catch {
      // Already gone or not in the abantu repo — nothing to do.
    }
  };

  process.on("exit", stop);
  process.on("SIGINT", () => {
    stop();
    process.exit(0);
  });
  process.on("SIGTERM", () => {
    stop();
    process.exit(1);
  });

  return {
    dispose: async () => {
      stop();
    },
  };
};
