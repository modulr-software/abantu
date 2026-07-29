import { access } from "node:fs/promises";
import { join } from "node:path";
import type { ExtensionAPI } from "@earendil-works/pi-coding-agent";

const RELOAD_TOOLS = new Set(["edit", "write"]);
const RELOAD_PREFIX = "\n[clj-reload]";

/**
 * After every `edit` / `write` tool call, run `bb scripts/reload.clj` against
 * the project's nREPL to invoke `(dev/reload)` via clj-reload. Appends the
 * reload's exit code + stdout/stderr to the tool result so the agent sees
 * whether the reload succeeded.
 */
export default function (pi: ExtensionAPI) {
  pi.on("tool_result", async (event, ctx) => {
    if (!RELOAD_TOOLS.has(event.toolName)) return;

    const cwd = ctx.cwd;
    const portFile = join(cwd, ".nrepl-port");
    try {
      await access(portFile);
    } catch {
      return {
        content: [
          ...event.content,
          {
            type: "text" as const,
            text: `${RELOAD_PREFIX} no .nrepl-port in ${cwd}; is ./nrepl.sh running?`,
          },
        ],
      };
    }

    const { code, stdout, stderr } = await pi.exec("bb", ["scripts/reload.clj"], {
      cwd,
      signal: ctx.signal,
    });

    const parts = [`${RELOAD_PREFIX} (exit ${code})`];
    if (stdout.trim()) parts.push(stdout.trim());
    if (stderr.trim()) parts.push(stderr.trim());

    return {
      content: [
        ...event.content,
        { type: "text" as const, text: "\n" + parts.join("\n") },
      ],
    };
  });
}
