---
title: Samples
---

Shared theme for Metreeca docs sites.

|Variable|Value|
|--------|-----|
|project.artifactId|${project.artifactId}|
|project.version|${project.version}|
|page.root| ${page.root}          |
| page.base          | ${page.base}          |
| page.path          |${page.path}|

# Headings

Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque semper erat non dignissim porta. Donec non tincidunt nulla. Donec tempus velit eu ipsum facilisis imperdiet quis ac ex. Nunc non sem vitae est blandit feugiat.


## Donec Non 

Donec non tincidunt nulla. Donec tempus velit eu ipsum facilisis imperdiet quis ac ex. Nunc non sem vitae est blandit feugiat.

### Sit Amet

 Curabitur ornare lacinia nulla, nec rhoncus elit efficitur quis. Quisque non volutpat dolor.

# Panels

## Code

```java
final class ${page.title} {

	public static void main(final String... args) {
		new Mark()

				.source(Paths.get("src/docs"))
				.target(Paths.get("target/docs"))

				.build();
				
	}

}
```

## Admonitions

!!! info
	 Curabitur ornare lacinia nulla, nec rhoncus elit efficitur quis. Quisque non volutpat dolor.

!!! warning
	Duis lacinia risus eget tincidunt viverra. Pellentesque fringilla, justo ut facilisis condimentum,
	tortor felis tincidunt felis, at convallis lorem orci a lacus.

# Typography

## Links

- [home](../index.md)

## Lists

- Lorem ipsum dolor sit amet, consectetur adipiscing elit.
- Aliquam condimentum neque id quam cursus mollis.
- Sed viverra risus non dui scelerisque pulvinar.


1. Integer id turpis mattis, aliquam neque tincidunt, porttitor orci.
2. Vestibulum at ante in tortor tincidunt tincidunt.
3. Pellentesque tincidunt nunc et urna efficitur egestas.


## Tables

| x | y |
|---|---|
| 1 | 2 |
| a | b |

