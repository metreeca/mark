---
title: "Samples"
trail: "Context"
---

**Table of Contents**

[TOC]

# Typography

Text can be **bold** or _italic_ and can contain :warning: :information_source: .

- [external](https://example.com/)
  - https://example.com/
- [internal](../index.md)
- [anchor](#tables)
- [redirect](redirect.md)
- [broken](broken.md)

# Blocks

## Paragraphs

Donec non tincidunt nulla. Donec tempus velit eu ipsum facilisis imperdiet quis ac ex. Nunc non sem vitae est blandit
feugiat.

Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque semper erat non dignissim porta. Donec non tincidunt
nulla. Donec tempus velit eu ipsum facilisis imperdiet quis ac ex. Nunc non sem vitae est blandit feugiat.

## Quotes

> Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque semper erat non dignissim porta. Donec non tincidunt nulla. Donec tempus velit eu ipsum facilisis imperdiet quis ac ex. Nunc non sem vitae est blandit feugiat.

## Rules

Lorem ipsum dolor sit amet, consectetur adipiscing elit.

---
Quisque semper erat non dignissim porta. Donec non tincidunt.

## Images

![Large Image](images/large.png#center)

![Small Image](images/small.png#right) Donec non tincidunt nulla. Donec tempus velit eu ipsum facilisis imperdiet quis ac
ex. Nunc non sem vitae est blandit feugiat.

## Lists

### Unordered

- Lorem ipsum dolor sit amet, consectetur adipiscing elit.
- Aliquam condimentum neque id quam cursus mollis.
- Sed viverra risus non dui scelerisque pulvinar.

### Ordered

1. Integer id turpis mattis, aliquam neque tincidunt, porttitor orci.
2. Vestibulum at ante in tortor tincidunt tincidunt.
3. Pellentesque tincidunt nunc et urna efficitur egestas.

### Tasks

- [ ] One
- [ ] two
- [x] three

### Definitions

Integer
: id turpis mattis, aliquam neque tincidunt, porttitor orci.

Vestibulum
: at ante in tortor tincidunt tincidunt.

Pellentesque
: tincidunt nunc et urna efficitur egestas.

### Nested

- level 1 item (ul)

  1. level 2 item (ol)
  1. level 2 item (ol)

  - level 3 item (ul)
  - level 3 item (ul)

- level 1 item (ul)

  1. level 2 item (ol)
  1. level 2 item (ol)

  - level 3 item (ul)
  - level 3 item (ul)

  1. level 4 item (ol)
  1. level 4 item (ol)

  - level 3 item (ul)
  - level 3 item (ul)

- level 1 item (ul)

## Tables

|Variable|Value|
|--------|-----|
|project.artifactId|${project.artifactId}|
|project.version|${project.version}|
|page.root| ${page.root}          |
| page.base          | ${page.base}          |
| page.path          |${page.path}|

## Sources

```java
final class Samples {

    public static void main(final String... args) {
        new Mark()

                .source(Paths.get("src/docs"))
                .target(Paths.get("target/docs"))

                .build();

    }

}
```

## Admonitions

!!! info "Info"  
Curabitur ornare lacinia nulla, nec rhoncus elit efficitur quis. Quisque non volutpat dolor.

!!! warning "Warning"  
Duis lacinia risus eget tincidunt viverra. Pellentesque fringilla, justo ut facilisis condimentum, tortor felis tincidunt
felis, at convallis lorem orci a lacus.
