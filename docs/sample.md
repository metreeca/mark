---
title: "Markdown Kitchen Sink"
---

# Text

Text can be **bold**, _italic_ or `code` and can contain :warning: :information_source: .

## Links

- [external](https://example.com/)
    - https://example.com/
- [internal](.)
    - [anchor](#tables)
- [broken](broken.md)

## Placeholders

Placeholder expression formatted like `{{meta.<name>}}` are replaced with the content defined by the
matching `<meta name="<name>" content="<content>">` tag in the HTML loader document, for instance
`\{{meta.version}}` with `{{meta.version}}`.

# Blocks

## Paragraphs

Donec non tincidunt nulla. Donec tempus velit eu ipsum facilisis imperdiet quis ac ex. Nunc non sem vitae est blandit
feugiat.

Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque semper erat non dignissim porta. Donec non tincidunt
nulla. Donec tempus velit eu ipsum facilisis imperdiet quis ac ex. Nunc non sem vitae est blandit feugiat.

## Quotes

> Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque semper erat non dignissim porta. Donec non tincidunt
> nulla. Donec tempus velit eu ipsum facilisis imperdiet quis ac ex. Nunc non sem vitae est blandit feugiat.

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

```
* generic
* source
* code
```

## Rules

Lorem ipsum dolor sit amet, consectetur adipiscing elit.

---
Quisque semper erat non dignissim porta. Donec non tincidunt.

# Lists

## Unordered

- Lorem ipsum dolor sit amet, consectetur adipiscing elit.
- Aliquam condimentum neque id quam cursus mollis.
- Sed viverra risus non dui scelerisque pulvinar.

## Ordered

1. Integer id turpis mattis, aliquam neque tincidunt, porttitor orci.
2. Vestibulum at ante in tortor tincidunt tincidunt.
3. Pellentesque tincidunt nunc et urna efficitur egestas.

## Tasks

- [ ] One
- [ ] two
- [x] three

## Nested

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

# Tables

| Variable                            | Value                                                                      |
|-------------------------------------|----------------------------------------------------------------------------|
| page.root                           | ${page.root}                                                               |
| page.base                           | ${page.base}                                                               |
| [links are not wrapped in tables]() | Donec non tincidunt nulla. Donec tempus velit eu ipsum facilisis imperdiet |

# Images

![Large Image](images/large.svg#75)

![Small Image](images/small.svg#right) Donec non tincidunt nulla. Donec tempus velit eu ipsum facilisis imperdiet quis
ac ex. Nunc non sem vitae est blandit feugiat.

# Alerts

Alert panels are supported with
the [GitHub syntax.](https://github.com/orgs/community/discussions/16925#discussion-4085374)

```mardown
> [!NOTE]  
> Highlights information that users should take into account, even when skimming.

> [!TIP]
> Optional information to help a user be more successful.

> [!IMPORTANT]  
> Crucial information necessary for users to succeed.

> [!WARNING]  
> Critical content demanding immediate user attention due to potential risks.

> [!CAUTION]
> Negative potential consequences of an action.
```

> [!NOTE]  
> Highlights information that users should take into account, even when skimming.

> [!TIP]
> Optional information to help a user be more successful.

> [!IMPORTANT]  
> Crucial information necessary for users to succeed.

> [!WARNING]  
> Critical content demanding immediate user attention due to potential risks.

> [!CAUTION]
> Negative potential consequences of an action.

# Headings

## Very Long Heading Larger Than Tray Width