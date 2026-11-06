import { Config } from "@remotion/cli/config";

// Use the installed Chrome; the bundled Chrome download can hang on first run.
Config.setBrowserExecutable("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
