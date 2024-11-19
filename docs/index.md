<!--- # Metreeca/Mark ---> 

[![npm](https://img.shields.io/npm/v/@metreeca/mark)](https://www.npmjs.com/package/@metreeca/mark)

Metreeca/Mark is a minimalist reader for static Markdown document sites. It doesn't require complex setups, specific
site layouts or release-time static site generation: just throw in a couple of Markdown pages and let the reader take
care of the details…

# Usage

## Create Content

Create Markdown content using your favourite editor; the following markdown extensions are supported:

- [YAML Frontmatter](https://github.com/remarkjs/remark-frontmatter?tab=readme-ov-file#remark-frontmatter)
- [GitHub Flavored Markdown](https://github.com/remarkjs/remark-gfm?tab=readme-ov-file#remark-gfm)
- [GitHub Alerts](https://github.com/jaywcjlove/remark-github-blockquote-alert?tab=readme-ov-file#remark-github-blockquote-alert)
- [GitHub Emojis](https://github.com/remarkjs/remark-gemoji?tab=readme-ov-file#remark-gemoji)

> [!WARNING]
>
> Make sure to use relative links to refer to images and related content.

## Define a Loader

Define a HTML file to be served by your deployment environment as fallback content for unknown routes. For GitHub Pages
that would be a `404.html` file in the root of your source branch/folder.

> [!WARNING]
>
> When deploying to GitHub Pages, make sure to include a `.nojekyll` file.

Include a `head` element according to the following (all `meta`/`link`elements are optional).

```html
<!DOCTYPE html>

<html lang="en">

    <head>

        <title>Metreeca/Mark</title>

        <meta name="version" content="{{meta.version}}">
        <meta name="description" content="A minimalist Markdown document reader">

        <meta name="creator" content="https://www.metreeca.com/">
        <meta name="copyright" content="&copy; 2020-2024 Metreeca">
        <meta name="license" content="Creative Commons BY-NC-SA 4.0 License">
        <meta name="license:uri" content="https://creativecommons.org/licenses/by-nc-sa/4.0/">

        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta name="theme-color" content="#786">

        <meta http-equiv="content-type" content="text/html; charset=UTF-8">
        <meta http-equiv="X-UA-Compatible" content="ie=edge">


        <!-- define if deployed to a subfolder -->

        <link rel="home" href="/mark/"/>


        <!-- `@{major}` and `@{major.minor}`) version ranges also supported -->

        <link rel="icon" type="image/svg+xml"
                href="https://cdn.jsdelivr.net/npm/@metreeca/mark@{{meta.version}}/dist/index.svg"/>

        <link rel="stylesheet" type="text/css"
                href="https://cdn.jsdelivr.net/npm/@metreeca/mark@{{meta.version}}/dist/index.css">

        <script type="module"
                src="https://cdn.jsdelivr.net/npm/@metreeca/mark@{{meta.version}}/dist/index.js"></script>


        <!-- include to define top-level navigation links in the sidebar -->

        <script type="application/json">

            {
                 "Tutorials": "/tutorials/",
                 "How-To": "/how-to/",
                 "GitHub": "https://github.com/metreeca/mark"
            }

        </script>

    </head>

</html>
```

### URL Rewriting

The loader will dynamically retrieve and render Markdown content as inferred from the current URL according to the
following patterns:

| URL						                    | Content							                      |
|------------------------------|-------------------------------------|
| https://example.com/		       | https://example.com/index.md		      |
| https://example.com/folder/  | https://example.com/folder/index.md |
| https://example.com/document | https://example.com/document.md	    |

Internal Markdown links in the *Content* format are automatically rewritten to the corresponding *URL* format.

### Meta Placeholders

The loader will dynamically replace placeholder expression formatted like `{{meta.<name>}}`  with the content defined by
the matching `<meta name="<name>" content="<content>">` tag in the HTML loader document, for instance `{{meta.version}}`
with `1.2.3`.

# Support

- open an [issue](https://github.com/metreeca/mark/issues) to report a problem or to suggest a new feature
- start a [discussion](https://github.com/metreeca/mark/discussions) to ask a how-to question or to share an idea

# License

This project is licensed under the Apache 2.0 License – see
[LICENSE](https://github.com/metreeca/mark/blob/main/LICENSE) file for details.