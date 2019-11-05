
[![Maven Central](https://img.shields.io/maven-central/v/com.metreeca/metreeca-mark.svg)](https://search.maven.org/artifact/com.metreeca/metreeca-mark/)

# metreeca/mark – the hassle-free static site generator

Metreeca/Mark is an minimalist static site generator, optimized for generating project/app docs. Unlike other solutions it's tightly integrated in the build process as a Maven plugin and doesn't force on you specific site layouts or complex setups: just throw in a couple of [Markdown](https://guides.github.com/features/mastering-markdown/#syntax) pages and a [Pug/Jade](https://naltatis.github.io/jade-syntax-docs/) template and let the generator take care of the details.

## Usage

### Configure the plugin

Add the pluging to your build configuration as:

```xml
 <build>
   <plugins>

        <plugin>

            <groupId>com.metreeca</groupId>
            <artifactId>mark-maven-plugin</artifactId>
            <version>0.0.0-SNAPSHOT</version>

            <executions>
                <execution>

                    <id>generate-docs</id>
                    <phase>generate-resources</phase>

                    <goals>
                        <goal>build</goal>
                    </goals>

                    <configuration>
                        <source>src/docs</source>
                        <target>target/docs</target>
                        <layout>assets/default.jade</layout>
                    </configuration>

                </execution>
            </executions>

        </plugin>

    </plugins>
</build>
```

The following configuration parameters are available:

| parameter | value                                                  | default       |
| --------- | ------------------------------------------------------ | ------------- |
| `source`  | the source folder for the site                         | `src/docs`    |
| `target`  | the target folder for the generated site               | `target/docs` |
| `layout`  | the path of default page template relative to `source` | required      |

### Define a default template

Define a default Pug template under the `source` folder at the relative path specified by the `layout` parameter, for instance as:

```jade
doctype html

html(lang="en")

    head

        title #{page.title} " | " #{project.name}
        
        ⋮
```

The following properties are available for inclusion using the `#/!{expression}` syntax:

| property        | value                                                        |
| --------------- | ------------------------------------------------------------ |
| `page.*`        | page front matter properties                                 |
| `page.date`     | the page date in ISO format; defaults to the current date, unless specified in the front matter |
| `page.base`     | the path of the site root relative to the page               |
| `page.path`     | the path of the page relative to the site root               |
| `page.headings` | a list of page [headings](https://javadoc.io/doc/com.vladsch.flexmark/flexmark/undefined/com/vladsch/flexmark/ast/Heading.html); use `heading.anchorRefId`, `heading.level` and `heading.text` to generate TOCs |
| `page.content`  | the content of the page rendered as HTML                     |
| `project.*`     | Maven project properties                                     |

**⚠︎** Pug templates are rendered using [jade4j](https://github.com/neuland/jade4j): expressions are evaluated as [JEXL](http://commons.apache.org/proper/commons-jexl/) rather than Javascript.

### Define site content

Define site pages as `.md` files under the `source` folder, for instance as:

```markdown
---
title: Lorem Ipsum
date: 2019-11-05 # optional
layout: post.pug # optional
---

Lorem ipsum ${project.version} dolor sit amet, consectetur adipiscing elit…
```

All of the properties available to templates (with the obvious exception of `page.content`) are also available for inclusion inside pages using the `${expression}` syntax.

The template to be used for rendering the page may be explicitely selected setting the `layout` front matter property to the path of the required template, relative to the plugin `layout` parameter.

**⚠︎** Markdown pages are parsed using [flexmark](https://github.com/vsch/flexmark-java): YAML front matter is supported with a [limited syntax](https://github.com/vsch/flexmark-java/wiki/Extensions#yaml-front-matter).

### Generate the site

```sh
mvn mark:build # or package
```

- the `target` folder is cleared
- `.md` files under the `source` folder are converted to `.html` files at the same relative path under the `target` folder, using the default Pug template specified by the `layout` parameter or by the `layout` front-matter property
- cross-links to `.md` files are converted to the corresponding `.html` file
-  templates (that is, files with the same estensions as the default template specified by the `layout` parameter) are ignored
- everything else under the `source` folder is copied verbatim to the same relative path under the `target` folder

```sh
mvn mark:watch
```

- the site is generated as detailed above
- on file updates and additions under the `source` folder, the corresponding files under the `target` folder are regenerated as required; if a template is modified,  the whole site is regenerated

## Support

- open an https://github.com/metreeca/mark/issues to report a problem or to suggest a new feature
- open a topic on [groups.google.com/d/forum/metreeca](https://groups.google.com/d/forum/metreeca) to ask how-to or open-ended questions

## License

This project is licensed under the MIT License – see [LICENSE](LICENSE) file for details
