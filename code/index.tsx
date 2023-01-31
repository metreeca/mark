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

import * as React from "react";
import { lazy, Suspense } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";


const MarkFile=lazy(() => import("@metreeca/mark/nests/file"));
const MarkPage=lazy(() => import("@metreeca/mark/tiles/page"));

createRoot(document.body.appendChild(document.createElement("mark-root"))).render((

	<React.StrictMode>

		<Suspense>

			<MarkFile>{file =>

				<Suspense>

					<MarkPage>{file}</MarkPage>

				</Suspense>

			}</MarkFile>

		</Suspense>

	</React.StrictMode>

));
