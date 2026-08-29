import { access } from "node:fs/promises";
import { join } from "node:path";

const RELOAD_TOOLS = new Set(["edit", "write"]);
const RELOAD_PREFIX = "\n[clj-reload]";

export const ReloadPlugin = async ({ worktree }) => {
  return {
    "tool.execute.after": async (input, output) => {
      if (!RELOAD_TOOLS.has(input.tool)) return;

      const cwd = worktree;
      const portFile = join(cwd, ".nrepl-port");
      try {
        await access(portFile);
      } catch {
        return;
      }

      let stdout = "";
      let stderr = "";
      let code = 0;
      try {
        const proc = Bun.spawn(["bb", "scripts/reload.clj"], {
          cwd,
          stdout: "pipe",
          stderr: "pipe",
        });
        stdout = (await new Response(proc.stdout).text()).trim();
        stderr = (await new Response(proc.stderr).text()).trim();
        code = await proc.exited;
      } catch (e) {
        code = 1;
        stderr = String(e);
      }

      const parts = [`${RELOAD_PREFIX} (exit ${code})`];
      if (stdout) parts.push(stdout);
      if (stderr) parts.push(stderr);

      output.output = output.output + "\n" + parts.join("\n");
    },
  };
};
