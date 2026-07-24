import type { Plugin } from "@opencode-ai/plugin"
import { spawn } from "child_process"
import { access } from "fs/promises"
import { join } from "path"

const RELOAD_TOOLS = new Set(["edit", "write"])
const RELOAD_PREFIX = "\n[clj-reload]"

export const Reload = (async ({ directory }) => {
  return {
    "tool.execute.after": async (input, output) => {
      if (!RELOAD_TOOLS.has(input.tool)) return

      const portFile = join(directory, ".nrepl-port")
      try {
        await access(portFile)
      } catch {
        output.output += `${RELOAD_PREFIX} no .nrepl-port in ${directory}; is ./nrepl.sh running?`
        return
      }

      await new Promise<void>((resolve) => {
        const proc = spawn("bb", ["scripts/reload.clj"], {
          cwd: directory,
          stdio: ["ignore", "pipe", "pipe"],
        })
        let stdout = ""
        let stderr = ""
        proc.stdout.on("data", (d) => (stdout += d.toString()))
        proc.stderr.on("data", (d) => (stderr += d.toString()))
        proc.on("error", (err) => {
          output.output += `${RELOAD_PREFIX} failed to spawn bb: ${err.message}`
          resolve()
        })
        proc.on("close", (code) => {
          const banner = `${RELOAD_PREFIX} (exit ${code})`
          const parts = [banner]
          if (stdout.trim()) parts.push(stdout.trim())
          if (stderr.trim()) parts.push(stderr.trim())
          output.output += "\n" + parts.join("\n")
          resolve()
        })
      })
    },
  }
}) satisfies Plugin

export default Reload

