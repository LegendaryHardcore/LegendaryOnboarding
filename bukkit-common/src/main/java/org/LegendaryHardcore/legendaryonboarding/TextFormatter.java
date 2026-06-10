package org.LegendaryHardcore.legendaryonboarding;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats config text using named variables and Minecraft legacy codes.
 */
public final class TextFormatter {
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>()]+");
    private static final Map<Character, NamedTextColor> LEGACY_COLORS = Map.ofEntries(
            Map.entry('0', NamedTextColor.BLACK),
            Map.entry('1', NamedTextColor.DARK_BLUE),
            Map.entry('2', NamedTextColor.DARK_GREEN),
            Map.entry('3', NamedTextColor.DARK_AQUA),
            Map.entry('4', NamedTextColor.DARK_RED),
            Map.entry('5', NamedTextColor.DARK_PURPLE),
            Map.entry('6', NamedTextColor.GOLD),
            Map.entry('7', NamedTextColor.GRAY),
            Map.entry('8', NamedTextColor.DARK_GRAY),
            Map.entry('9', NamedTextColor.BLUE),
            Map.entry('a', NamedTextColor.GREEN),
            Map.entry('b', NamedTextColor.AQUA),
            Map.entry('c', NamedTextColor.RED),
            Map.entry('d', NamedTextColor.LIGHT_PURPLE),
            Map.entry('e', NamedTextColor.YELLOW),
            Map.entry('f', NamedTextColor.WHITE)
    );
    private static final Map<String, NamedTextColor> COLOR_VARIABLES = Map.ofEntries(
            Map.entry("black", NamedTextColor.BLACK),
            Map.entry("dark_blue", NamedTextColor.DARK_BLUE),
            Map.entry("dark_green", NamedTextColor.DARK_GREEN),
            Map.entry("dark_aqua", NamedTextColor.DARK_AQUA),
            Map.entry("dark_red", NamedTextColor.DARK_RED),
            Map.entry("dark_purple", NamedTextColor.DARK_PURPLE),
            Map.entry("gold", NamedTextColor.GOLD),
            Map.entry("gray", NamedTextColor.GRAY),
            Map.entry("grey", NamedTextColor.GRAY),
            Map.entry("dark_gray", NamedTextColor.DARK_GRAY),
            Map.entry("dark_grey", NamedTextColor.DARK_GRAY),
            Map.entry("blue", NamedTextColor.BLUE),
            Map.entry("green", NamedTextColor.GREEN),
            Map.entry("aqua", NamedTextColor.AQUA),
            Map.entry("red", NamedTextColor.RED),
            Map.entry("light_purple", NamedTextColor.LIGHT_PURPLE),
            Map.entry("yellow", NamedTextColor.YELLOW),
            Map.entry("white", NamedTextColor.WHITE)
    );
    private static final Map<String, TextDecoration> DECORATION_VARIABLES = Map.ofEntries(
            Map.entry("bold", TextDecoration.BOLD),
            Map.entry("italic", TextDecoration.ITALIC),
            Map.entry("italics", TextDecoration.ITALIC),
            Map.entry("underlined", TextDecoration.UNDERLINED),
            Map.entry("underline", TextDecoration.UNDERLINED),
            Map.entry("strikethrough", TextDecoration.STRIKETHROUGH),
            Map.entry("obfuscated", TextDecoration.OBFUSCATED),
            Map.entry("magic", TextDecoration.OBFUSCATED)
    );
    private static final Map<Character, TextDecoration> LEGACY_DECORATIONS = Map.of(
            'k', TextDecoration.OBFUSCATED,
            'l', TextDecoration.BOLD,
            'm', TextDecoration.STRIKETHROUGH,
            'n', TextDecoration.UNDERLINED,
            'o', TextDecoration.ITALIC
    );

    private TextFormatter() {
    }

    public static Component format(String input) {
        return format(input, false);
    }

    public static Component formatChat(String input) {
        return format(input, true);
    }

    private static Component format(String input, boolean linkify) {
        String text = input == null ? "" : input.replace("\\n", "\n");
        TextComponent.Builder output = Component.text();
        StringBuilder segment = new StringBuilder();
        FormatState state = new FormatState();

        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);

            int consumed = consumeLegacyCode(text, index, output, segment, state, linkify);
            if (consumed > 0) {
                index += consumed;
                continue;
            }

            if (character == '{') {
                int closingBrace = text.indexOf('}', index + 1);
                if (closingBrace > index) {
                    String variable = text.substring(index + 1, closingBrace)
                            .toLowerCase(Locale.ROOT);
                    if (applyVariable(variable, output, segment, state, linkify)) {
                        index = closingBrace;
                        continue;
                    }
                }
            }

            segment.append(character);
        }

        append(output, segment, state, linkify);
        return output.build();
    }

    private static int consumeLegacyCode(
            String text,
            int index,
            TextComponent.Builder output,
            StringBuilder segment,
            FormatState state,
            boolean linkify
    ) {
        char marker = text.charAt(index);
        if ((marker != '&' && marker != '\u00a7') || index + 1 >= text.length()) {
            return 0;
        }

        char code = Character.toLowerCase(text.charAt(index + 1));
        NamedTextColor color = LEGACY_COLORS.get(code);
        if (color != null) {
            append(output, segment, state, linkify);
            state.setColor(color);
            state.clearDecorations();
            return 1;
        }

        TextDecoration decoration = LEGACY_DECORATIONS.get(code);
        if (decoration != null) {
            append(output, segment, state, linkify);
            state.enable(decoration);
            return 1;
        }

        if (code == 'r') {
            append(output, segment, state, linkify);
            state.reset();
            return 1;
        }

        if (code == 'x') {
            TextColor hexColor = parseLegacyHex(text, index, marker);
            if (hexColor != null) {
                append(output, segment, state, linkify);
                state.setColor(hexColor);
                state.clearDecorations();
                return 13;
            }
        }

        if (code == '#' && index + 7 < text.length()) {
            TextColor hexColor = TextColor.fromHexString(text.substring(index + 1, index + 8));
            if (hexColor != null) {
                append(output, segment, state, linkify);
                state.setColor(hexColor);
                state.clearDecorations();
                return 7;
            }
        }

        return 0;
    }

    private static TextColor parseLegacyHex(String text, int index, char marker) {
        if (index + 13 >= text.length()) {
            return null;
        }

        StringBuilder hex = new StringBuilder("#");
        for (int offset = 2; offset <= 12; offset += 2) {
            if (text.charAt(index + offset) != marker) {
                return null;
            }
            hex.append(text.charAt(index + offset + 1));
        }
        return TextColor.fromHexString(hex.toString());
    }

    private static boolean applyVariable(
            String variable,
            TextComponent.Builder output,
            StringBuilder segment,
            FormatState state,
            boolean linkify
    ) {
        NamedTextColor color = COLOR_VARIABLES.get(variable);
        if (color != null) {
            append(output, segment, state, linkify);
            state.setColor(color);
            return true;
        }

        if (variable.startsWith("#")) {
            TextColor hexColor = TextColor.fromHexString(variable);
            if (hexColor != null) {
                append(output, segment, state, linkify);
                state.setColor(hexColor);
                return true;
            }
        }

        TextDecoration decoration = DECORATION_VARIABLES.get(variable);
        if (decoration != null) {
            append(output, segment, state, linkify);
            state.enable(decoration);
            return true;
        }

        if (variable.equals("newline")) {
            segment.append(linkify ? '\n' : ' ');
            return true;
        }

        if (variable.equals("standard") || variable.equals("plain")) {
            append(output, segment, state, linkify);
            state.clearDecorations();
            return true;
        }

        if (variable.equals("reset")) {
            append(output, segment, state, linkify);
            state.reset();
            return true;
        }

        return false;
    }

    private static void append(
            TextComponent.Builder output,
            StringBuilder segment,
            FormatState state,
            boolean linkify
    ) {
        if (segment.isEmpty()) {
            return;
        }

        String text = segment.toString();
        if (!linkify) {
            output.append(Component.text(text).style(state.style()));
            segment.setLength(0);
            return;
        }

        Matcher matcher = URL_PATTERN.matcher(text);
        int previousEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > previousEnd) {
                output.append(Component.text(text.substring(previousEnd, matcher.start())).style(state.style()));
            }

            String url = trimTrailingPunctuation(matcher.group());
            int urlEnd = matcher.start() + url.length();
            output.append(Component.text(url)
                    .style(state.style())
                    .clickEvent(ClickEvent.openUrl(url))
                    .hoverEvent(HoverEvent.showText(Component.text("Open link"))));
            if (urlEnd < matcher.end()) {
                output.append(Component.text(text.substring(urlEnd, matcher.end())).style(state.style()));
            }
            previousEnd = matcher.end();
        }
        if (previousEnd < text.length()) {
            output.append(Component.text(text.substring(previousEnd)).style(state.style()));
        }
        segment.setLength(0);
    }

    private static String trimTrailingPunctuation(String url) {
        int end = url.length();
        while (end > 0 && ".,!?;:".indexOf(url.charAt(end - 1)) >= 0) {
            end--;
        }
        return url.substring(0, end);
    }

    private static final class FormatState {
        private TextColor color = NamedTextColor.WHITE;
        private final Style.Builder style = Style.style();

        private void setColor(TextColor color) {
            this.color = color;
        }

        private void enable(TextDecoration decoration) {
            style.decoration(decoration, TextDecoration.State.TRUE);
        }

        private void clearDecorations() {
            for (TextDecoration decoration : TextDecoration.values()) {
                style.decoration(decoration, TextDecoration.State.FALSE);
            }
        }

        private void reset() {
            color = NamedTextColor.WHITE;
            clearDecorations();
        }

        private Style style() {
            return style.color(color).build();
        }
    }
}
