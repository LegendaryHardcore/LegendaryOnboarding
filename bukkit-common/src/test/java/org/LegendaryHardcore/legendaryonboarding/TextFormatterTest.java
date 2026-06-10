package org.LegendaryHardcore.legendaryonboarding;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextFormatterTest {
    @Test
    void formatsNamedColorsAndDecorations() {
        Component expected = Component.text()
                .append(Component.text("Important", NamedTextColor.RED, TextDecoration.BOLD))
                .append(plain(" normal", NamedTextColor.RED))
                .build();

        assertEquals(
                expected,
                TextFormatter.format("{red}{bold}Important{standard} normal")
        );
    }

    @Test
    void formatsEveryMinecraftDecoration() {
        Component expected = Component.text()
                .append(Component.text(
                        "Styled",
                        NamedTextColor.WHITE,
                        TextDecoration.BOLD,
                        TextDecoration.ITALIC,
                        TextDecoration.UNDERLINED,
                        TextDecoration.STRIKETHROUGH,
                        TextDecoration.OBFUSCATED
                ))
                .build();

        assertEquals(
                expected,
                TextFormatter.format(
                        "{bold}{italic}{underlined}{strikethrough}{obfuscated}Styled"
                )
        );
    }

    @Test
    void supportsAmpersandAndSectionLegacyCodes() {
        Component expected = Component.text()
                .append(plain("Red ", NamedTextColor.RED))
                .append(plain("Bold", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.BOLD, true))
                .append(plain(" reset", NamedTextColor.WHITE))
                .build();

        assertEquals(expected, TextFormatter.format("&cRed \u00a7e\u00a7lBold&r reset"));
    }

    @Test
    void supportsNamedAndLegacyHexColors() {
        Component expected = Component.text()
                .append(Component.text("Named ", TextColor.color(0x12ABEF)))
                .append(plain("Short ", TextColor.color(0xABCDEF)))
                .append(plain("Minecraft", TextColor.color(0x123456)))
                .build();

        assertEquals(
                expected,
                TextFormatter.format(
                        "{#12ABEF}Named &#ABCDEFShort \u00a7x\u00a71\u00a72\u00a73\u00a74\u00a75\u00a76Minecraft"
                )
        );
    }

    @Test
    void preservesUnknownVariablesAndConvertsConfiguredNewlines() {
        assertEquals(
                Component.text()
                        .append(Component.text("{player}\nNext", NamedTextColor.WHITE))
                        .build(),
                TextFormatter.format("{player}\\nNext")
        );
    }

    @Test
    void replacesNewlineVariableWithSpaceInTitleText() {
        assertEquals(
                Component.text()
                        .append(Component.text(
                                "First Second",
                                NamedTextColor.GOLD,
                                TextDecoration.BOLD
                        ))
                        .build(),
                TextFormatter.format("{gold}{bold}First{newline}Second")
        );
    }

    @Test
    void supportsNewlineVariableInChat() {
        assertEquals(
                Component.text()
                        .append(Component.text("First\nSecond", NamedTextColor.WHITE))
                        .build(),
                TextFormatter.formatChat("First{newline}Second")
        );
    }

    @Test
    void makesChatUrlsClickableWithoutConsumingPunctuation() {
        Component expected = Component.text()
                .append(Component.text("Rules: ", NamedTextColor.YELLOW))
                .append(Component.text("https://example.com/rules", NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.openUrl("https://example.com/rules"))
                        .hoverEvent(HoverEvent.showText(Component.text("Open link"))))
                .append(Component.text(".", NamedTextColor.AQUA))
                .build();

        assertEquals(
                expected,
                TextFormatter.formatChat("{yellow}Rules: {aqua}https://example.com/rules.")
        );
    }

    @Test
    void makesMultipleEmbeddedChatUrlsClickable() {
        Component formatted = TextFormatter.formatChat(
                "Rules: https://example.com/rules | Map: https://map.example.com"
        );

        assertEquals(
                ClickEvent.openUrl("https://example.com/rules"),
                formatted.children().get(1).clickEvent()
        );
        assertEquals(
                ClickEvent.openUrl("https://map.example.com"),
                formatted.children().get(3).clickEvent()
        );
    }

    private static Component plain(String text, TextColor color) {
        return Component.text(text, color)
                .decoration(TextDecoration.BOLD, false)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.UNDERLINED, false)
                .decoration(TextDecoration.STRIKETHROUGH, false)
                .decoration(TextDecoration.OBFUSCATED, false);
    }
}
