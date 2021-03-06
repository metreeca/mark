[![Maven Central](https://img.shields.io/maven-central/v/com.metreeca/mark-maven-plugin.svg)](https://search.maven.org/artifact/com.metreeca/mark-maven-plugin/)

# metreeca/mark

Metreeca/Mark is an minimalist static site generator, optimized for project/app docs. Unlike other solutions it's tightly integrated in the Maven build process as a plugin and doesn't force on you specific site layouts or complex setups: just throw in a couple of Markdown pages and a Pug/Less template and let the generator take care of the details…

# Usage

## Configure the plugin

Add the plugin to your build configuration as:

```xml
 <build>
   <plugins>

        <plugin>

            <groupId>com.metreeca</groupId>
            <artifactId>mark-maven-plugin</artifactId>
            <version>${mark.version}</version>

            <configuration> <!-- optional -->
                
                <source>src/docs</source>
                <target>target/docs</target>
                <assets>@/docs</assets>
                <layout>layouts/default.pug</layout>
                
                <options> <!-- pipeline specific options --> 
                    <pipeline-option>value</pipeline-option>
                </options>
                
            </configuration>
            
            <executions>
                <execution> <!-- example -->
                    
                    <goals>
                        <goal>clean</goal>
                        <goal>build</goal>
                        <goal>check</goal>
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
| `source`  | `src/docs`    | the source folder for the site                         |
| `target`  | `target/docs` | the target folder for the generated site               |
| `assets` | a virtual empty folder | the path of an additional asset folder |
| `layout`  | `layouts/default.pug`| the path of default page template relative to `source` or `assets` |

The plugin binds by default the `build` goal to the `site` maven phase.

## Define a template

> ℹ️
> If you feel lazy, use the default bundled docs theme including  `<assets>@/docs</assets>` in the configuration

Define a default Pug template under the `source` folder at the relative path specified by the `layout` parameter, for instance:

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
| `page.date`     | the page date in ISO format; defaults to the current date, unless specified in the front matter |
| `page.root`     | the path of the site root relative to the page               |
| `page.base` | the path of the page folder relative to the site root |
| `page.path`     | the path of the page relative to the site root               |
| `page.headings` | a list of page [headings](https://javadoc.io/doc/com.vladsch.flexmark/flexmark/undefined/com/vladsch/flexmark/ast/Heading.html); use `heading.anchorRefId`, `heading.level` and `heading.text` to generate TOCs |
| `page.body`  | page content rendered as HTML                     |
| `pages[]` | a list containing  `page` objects for all HTML pages |
| `project.*`     | Maven project properties                                     |

> :warning: 
> **Pug templates are rendered using [pug4j](https://github.com/neuland/pug4j): expressions are evaluated as [JEXL](http://commons.apache.org/proper/commons-jexl/) rather than Javascript.**

## Create site content

Define site pages as `.md` files under the `source` folder, for instance as:

```markdown
---
title: Lorem Ipsum
date: 2019-11-05 # optional
layout: post.pug # optional
---

Lorem ipsum ${project.version} dolor sit amet, consectetur adipiscing elit…
```

All the properties available to templates (with the obvious exception of `page.body`) are also available for interpolation inside pages using the `${expression}` syntax (escape like `\${expression}` to include verbatim). 

The template to be used for rendering the page may be explicitly selected by setting the `layout` front matter property to the path of the required template, relative to the plugin `layout` parameter.

> :warning: 
> **Markdown pages are parsed using [flexmark](https://github.com/vsch/flexmark-java): YAML front matter is supported with a [limited syntax](https://github.com/vsch/flexmark-java/wiki/Extensions#yaml-front-matter).**

# Goals

## Clean site

```shell
mvn mark:clean # by default in the pre-clean phase
```

- the `target` folder is deleted

## Generate site

```shell
mvn mark:build # by default in the pre-site phase
```

- files matching one of the following files extensions in  the `source` folder are processed by the corresponding pipeline
- templates (that is, files with the same extensions as the default template specified by the `layout` parameter) are ignored
- everything else under the `source` folder is copied verbatim to the same relative path under the `target` folder

**markdown** / `.md` files under the `source` folder are converted to `.html` files at the same relative path under the `target` folder, using the default Pug template specified by the `layout` parameter or by the `layout` front-matter property; links to other `.md` files are converted to the corresponding `.html` file.

| option                    | default | behaviour                                             |
| ------------------------- | ------- | ----------------------------------------------------- |
| `markdown-smart-links`    | `false` | removes `.html` and `/index.html` suffixes from links |
| `markdown-external-links` | `false` | opens extenal links in a `_blank` target              |

**less/css** › `.css` / `.less` and `.css` files under the source folder are converted to minified `.css` files at the same relative path under the target.

## Check links

```shell
mvn mark:check # by default in the post-site phase
```

- HTML files under the `target` folder are scanned and dangling links reported

## Watch site sources

```shell
mvn mark:watch
```

- the site is generated as described above
- on file updates and additions under the `source` folder, the corresponding files under the `target` folder are regenerated as required; if a template is modified, the whole site is regenerated

## Serve generated site

```shell
mvn mark:serve
```

- the site is generated and watched as described above
- the generated site is served on a development grade HTTP server for testing purposes
- on supported systems, the served site is automatically opened in the default user browser
- pages are automatically reloaded on updates (courtesy of [Live.js](https://livejs.com)

> :warning:
> **Live page reloading assumes `UTF-8`.**

# Support

- open an [issue](https://github.com/metreeca/mark/issues) to report a problem or to suggest a new feature
- open a topic on [groups.google.com/d/forum/metreeca](https://groups.google.com/d/forum/metreeca) to ask how-to or open-ended questions

# License

This project is licensed under the MIT License – see [LICENSE](LICENSE) file for details
