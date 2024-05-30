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

"use strict";

import fs from "fs";
import path from "path";

const json = "package.json";
const index = "index.md";
const readme = "README.md";
const license = "LICENSE";

const docs = path.resolve("docs");
const dist = path.resolve("dist");


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

// copy package.json

fs.copyFileSync(path.resolve(json), path.resolve(dist, json));

// copy license

fs.copyFileSync(path.resolve(license), path.resolve(dist, license));

// copy readme

fs.copyFileSync(path.resolve(docs, index), path.resolve(dist, readme));
