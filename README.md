[![Maven Central](https://img.shields.io/maven-central/v/com.metreeca/mark-maven-plugin.svg)](https://search.maven.org/artifact/com.metreeca/mark-maven-plugin/)

# Metreeca/Mark

Metreeca/Mark is a minimalist Maven plugin for static site generation. It is tightly integrated in the build process and
doesn't require specific site layouts or complex setups: just throw in a couple of Markdown pages and maybe a Pug/Less
template and let the generator take care of the details…

# Usage

## Configure the plugin

Add the plugin to your build configuration as:

```xml 
<build>

    <plugins>

        <plugin>

            <groupId>com.metreeca</groupId>
            <artifactId>mark-maven-plugin</artifactId>
            <version>0.8.7</version>

            <configuration> <!-- optional -->

                <source>src/docs</source>
                <target>target/docs</target>
                <layout>layouts/default.pug</layout>

                <readme>true</readme>

                <options> <!-- pipeline specific options -->
                    <pipeline-option>value</pipeline-option>
                </options>

            </configuration>

            <executions>
                <execution>

                    <goals>
                        <goal>clean</goal> <!-- by default in the pre-clean phase-->
                        <goal>build</goal> <!-- by default in the pre-site phase-->
                    </goals>

                </execution>
            </executions>

        </plugin>

    </plugins>

</build>
```

The following optional configuration parameters are available:

| parameter | default       | value                                                  |
| --------- | ------------- | ------------------------------------------------------ |
| `source`  | `./docs`    | the source folder for the site                         |
| `target`  | `${source}`| the target folder for the generated site<br/>defaults to in-place generation|
| `layout`  |  | the path of default page template relative to `${source}`<br/>defaults to a bundled docs layout |
| `readme`  | `false`  | generates a `README.md` file in the base project folder from `${source}/index.md`<br/>relative links are rewritten to `${project.distributionManagement.site.url}`, if defined in `pom.xml`, or to the original location under the `target` folder, otherwise |

## Define a template

> :information_source:
>
> If you feel lazy, just omit the `layout` parameter and place an `index.svg` icon in the source
> folder to use the default [bundled docs layout](https://metreeca.github.io/mark/samples/) ;-)

Define a default Pug template under the `source` folder at the relative path specified by the `layout` parameter, for
instance:

```pug
doctype html

html(lang="en")

    head

        title #{page.title} " | " #{project.name}

        meta(charset="UTF-8")

    body

        header
            h1 Example Site

        main !{page.body}

        footer
            small © 2020 Example
```

The following properties are available for inclusion using the `#/!{expression}` syntax:

| property        | value                                                        |
| --------------- | ------------------------------------------------------------ |
| `page.*`        | page front matter properties                                 |
| `page.date`     | the page date in ISO format (`yyyy-mm-dd`); defaults to the current date, unless specified in the front matter |
| `page.root`     | the path of the site root relative to the page               |
| `page.base` | the path of the page folder relative to the site root |
| `page.path`     | the path of the page relative to the site root               |
| `page.headings` | a list of page (use `heading.level`, `heading.id` and `heading.text` to generate TOCs |
| `page.body`  | page content rendered as HTML                     |
| `pages[]` | a list containing  `page` objects for all HTML pages |
| `project.*`     | Maven project properties                                     |

> :warning:
>
> Pug templates are rendered using [pug4j](https://github.com/neuland/pug4j): expressions are evaluated as
> [JEXL](http://commons.apache.org/proper/commons-jexl/)
> rather than Javascript. Expression interpolation in HTML attributes is supported as ``${expression}``

## Create site content

Define site pages as `.md` files under the `source` folder, for instance as:

```markdown
---
title: Lorem Ipsum
---

Lorem ipsum `${project.version}` dolor sit amet, consectetur adipiscing elit…
```

All the properties available to templates (with the obvious exception of `page.body`) are also available for
interpolation inside pages using the `${expression}` syntax (escape like `${expression}` to include verbatim).

The template to be used for rendering the page may be explicitly selected by setting the `layout` front matter property
to the path of the required template, relative to the plugin `layout` parameter.

> :warning:
>
> Markdown pages are parsed using [flexmark](https://github.com/vsch/flexmark-java): YAML front matter is
> supported with a [limited syntax](https://github.com/vsch/flexmark-java/wiki/Extensions#yaml-front-matter).

# Maven Goals

## Clean

```shell
mvn mark:clean # by default in the pre-clean phase
```

- the `target` folder is deleted

## Build

```shell
mvn mark:build # by default in the pre-site phase
```

- supported files in the `source` folder are processed by the matching [file pipeline](https://metreeca.github.io/mark/#file-pipelines)
- templates (that is, files with the same extensions as the default template specified by the `layout` parameter) are
  ignored
- everything else under the `source` folder is copied verbatim to the same relative path under the `target` folder
- HTML files under the `target` folder are scanned and dangling links reported

> :warning:
>
> When generating sites in-place, files converted without altering the file extension (for instance,
> `.css` › `.css`) are silently ignored.

## Watch

```shell
mvn mark:watch
```

- the site is generated as described above
- on file updates and additions under the `source` folder, the corresponding files under the `target` folder are
  regenerated as required; if a template is modified, the whole site is regenerated

## Serve

```shell
mvn mark:serve
```

- the site is generated and watched as described above
- the generated site is served on an embedded HTTP server for testing purposes
- on supported systems, the served site is automatically opened in the default user browser
- pages are automatically reloaded on updates

# File Pipelines

## Markdown

Converts `.md` files under the `source` folder to `.html` files at the same relative path under the `target` folder,
using the default Pug template specified by the `layout` parameter or by the `layout` front-matter property; links to
other `.md` files are converted to the corresponding `.html` file.

| option                    | default | behaviour                                            |
| ------------------------- | ------- | ---------------------------------------------------- |
| `markdown-smart-links`    | `false` | removes `.html` and `index.html` suffixes from links |
| `markdown-external-links` | `false` | opens external links in a `_blank` target            |

## Less/CSS

Converts `.less` and `.css` files under the `source` folder to minified `.css` files at the same relative path under
the `target` folder.

# Support

- open an [issue](https://github.com/metreeca/mark/issues) to report a problem or to suggest a new feature
- start a [discussion](https://github.com/metreeca/mark/discussions) to ask a how-to question or to share an idea

# License

This project is licensed under the Apache 2.0 License – see [LICENSE](https://github.com/metreeca/mark/blob/main/LICENSE)
file for details.
