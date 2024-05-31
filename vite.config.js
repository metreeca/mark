/*
 * Copyright © 2020-2024 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {defineConfig} from "vite";
import {resolve} from "node:path";
import react from "@vitejs/plugin-react";
import postcssNesting from "postcss-nesting";

const src = resolve(process.env.src || "code");
const etc = resolve(process.env.etc || "docs");
const out = resolve(process.env.out || "dist");

export default defineConfig(({mode}) => ({ // https://vitejs.dev/config/

    root: resolve(src),
    publicDir: resolve(etc),

    plugins: [react()],

    css: {
        postcss: {
            plugins: [postcssNesting()]
        }
    },

    resolve: {
        alias: {
            "@metreeca/mark": resolve(src)
        }
    },

    build: {

        outDir: out,
        assetsDir: ".",
        emptyOutDir: true,
        copyPublicDir: false,
        minify: mode !== "development",

        chunkSizeWarningLimit: 1024,

        rollupOptions: {
            output: {
                entryFileNames: `[name].js`,
                chunkFileNames: `[name].js`,
                assetFileNames: `[name].[ext]`
            }
        }

    }

}));
