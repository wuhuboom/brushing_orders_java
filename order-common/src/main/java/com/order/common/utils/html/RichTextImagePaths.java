package com.order.common.utils.html;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Converts managed upload image attributes without reserializing or sanitizing the HTML.
 * The /profile/ path namespace identifies managed uploads, including historical proxy prefixes.
 * JSON configuration values are supported; only string values containing image markup change.
 */
public final class RichTextImagePaths
{
    private static final String PROFILE = "/profile/";
    private static final ObjectMapper JSON = new ObjectMapper();

    private RichTextImagePaths()
    {
    }

    public static String toRelative(String value)
    {
        return convert(value, RichTextImagePaths::relativeUrl);
    }

    /** Existing absolute image URLs are deliberately retained for legacy clients. */
    public static String toAbsolute(String value, String baseUrl)
    {
        if (baseUrl == null || baseUrl.trim().isEmpty())
        {
            return value;
        }
        String base = baseUrl.trim();
        while (base.endsWith("/"))
        {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty())
        {
            return value;
        }
        final String prefix = base;
        return convert(value, url -> {
            // A browser resolves //host/path as an absolute, protocol-relative URL as well.
            if (!url.startsWith("/") || url.startsWith("//"))
            {
                return url;
            }
            String path = managedPath(url);
            return path == null ? url : prefix + path;
        });
    }

    private static String convert(String value, UnaryOperator<String> transform)
    {
        if (value == null || value.isEmpty())
        {
            return value;
        }
        String stripped = value.stripLeading();
        if (stripped.startsWith("{") || stripped.startsWith("[") || stripped.startsWith("\""))
        {
            return convertJson(value, transform);
        }
        return convertHtml(value, transform);
    }

    private static String convertJson(String value, UnaryOperator<String> transform)
    {
        List<Replacement> replacements = new ArrayList<>();
        try (JsonParser parser = JSON.getFactory().createParser(value))
        {
            int rootValues = 0;
            int depth = 0;
            JsonToken token;
            while ((token = parser.nextToken()) != null)
            {
                if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY)
                {
                    if (depth == 0) rootValues++;
                    depth++;
                }
                else if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) depth--;
                else if (depth == 0) rootValues++;
                if (rootValues > 1)
                {
                    return value;
                }
                if (token != JsonToken.VALUE_STRING)
                {
                    continue;
                }
                int start = (int) parser.getTokenLocation().getCharOffset();
                String original = parser.getText();
                String converted = convertHtml(original, transform);
                if (!original.equals(converted))
                {
                    int end = (int) parser.getCurrentLocation().getCharOffset();
                    replacements.add(new Replacement(start, end, JSON.writeValueAsString(converted)));
                }
            }
            return replace(value, replacements);
        }
        catch (IOException | RuntimeException ignored)
        {
            // Malformed configuration is never partially rewritten or treated as raw HTML.
            return value;
        }
    }

    private static String convertHtml(String html, UnaryOperator<String> transform)
    {
        List<Replacement> replacements = new ArrayList<>();
        int cursor = 0;
        int pictures = 0;
        while (cursor < html.length())
        {
            int start = html.indexOf('<', cursor);
            if (start < 0)
            {
                break;
            }
            if (html.startsWith("<!--", start))
            {
                int end = html.indexOf("-->", start + 4);
                if (end < 0) break;
                cursor = end + 3;
                continue;
            }
            if (html.startsWith("<![CDATA[", start))
            {
                int end = html.indexOf("]]>", start + 9);
                if (end < 0) break;
                cursor = end + 3;
                continue;
            }
            if (html.startsWith("<!", start) || html.startsWith("<?", start))
            {
                int end = tagEnd(html, start + 2);
                if (end < 0) break;
                cursor = end + 1;
                continue;
            }
            int nameStart = start + 1;
            boolean closing = nameStart < html.length() && html.charAt(nameStart) == '/';
            if (closing) nameStart++;
            int nameEnd = nameStart;
            if (nameEnd < html.length() && isLetter(html.charAt(nameEnd)))
            {
                while (nameEnd < html.length() && isName(html.charAt(nameEnd))) nameEnd++;
            }
            if (nameEnd == nameStart)
            {
                cursor = start + 1;
                continue;
            }
            int end = tagEnd(html, nameEnd);
            if (end < 0) break;
            String name = html.substring(nameStart, nameEnd).toLowerCase(Locale.ROOT);
            cursor = end + 1;
            if (closing)
            {
                if ("picture".equals(name) && pictures > 0) pictures--;
                continue;
            }
            if ("img".equals(name) || ("source".equals(name) && pictures > 0))
            {
                attributes(html, nameEnd, end, "img".equals(name), transform, replacements);
            }
            if ("picture".equals(name)) pictures++;
            if ("plaintext".equals(name)) break;
            if (isRawText(name))
            {
                int close = closingTag(html, name, cursor);
                if (close < 0) break;
                cursor = close;
            }
        }
        return replace(html, replacements);
    }

    private static boolean isRawText(String name)
    {
        return "script".equals(name) || "style".equals(name) || "textarea".equals(name)
                || "title".equals(name) || "xmp".equals(name) || "iframe".equals(name)
                || "noembed".equals(name) || "noframes".equals(name);
    }

    private static int closingTag(String html, String name, int from)
    {
        String marker = "</" + name;
        for (int i = from; i + marker.length() <= html.length(); i++)
        {
            if (html.charAt(i) == '<' && html.regionMatches(true, i, marker, 0, marker.length()))
            {
                int after = i + marker.length();
                if (after == html.length() || isSpace(html.charAt(after)) || html.charAt(after) == '>')
                {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int tagEnd(String html, int from)
    {
        char quote = 0;
        for (int i = from; i < html.length(); i++)
        {
            char c = html.charAt(i);
            if (quote != 0)
            {
                if (c == quote) quote = 0;
            }
            else if (c == '\'' || c == '"') quote = c;
            else if (c == '>') return i;
        }
        return -1;
    }

    private static void attributes(String html, int from, int end, boolean image,
            UnaryOperator<String> transform, List<Replacement> replacements)
    {
        int cursor = from;
        while (cursor < end)
        {
            while (cursor < end && (isSpace(html.charAt(cursor)) || html.charAt(cursor) == '/')) cursor++;
            int nameStart = cursor;
            while (cursor < end && !isSpace(html.charAt(cursor)) && html.charAt(cursor) != '='
                    && html.charAt(cursor) != '/' && html.charAt(cursor) != '>') cursor++;
            if (cursor == nameStart)
            {
                cursor++;
                continue;
            }
            String name = html.substring(nameStart, cursor);
            while (cursor < end && isSpace(html.charAt(cursor))) cursor++;
            if (cursor >= end || html.charAt(cursor) != '=') continue;
            cursor++;
            while (cursor < end && isSpace(html.charAt(cursor))) cursor++;
            if (cursor >= end) break;
            char quote = html.charAt(cursor);
            boolean quoted = quote == '\'' || quote == '"';
            if (quoted) cursor++;
            int valueStart = cursor;
            while (cursor < end && (quoted ? html.charAt(cursor) != quote : !isSpace(html.charAt(cursor)))) cursor++;
            int valueEnd = cursor;
            if (quoted && cursor < end) cursor++;
            if (image && "src".equalsIgnoreCase(name))
            {
                imageUrl(html, valueStart, valueEnd, transform, replacements);
            }
            else if ("srcset".equalsIgnoreCase(name))
            {
                srcset(html, valueStart, valueEnd, transform, replacements);
            }
        }
    }

    private static void srcset(String html, int from, int end, UnaryOperator<String> transform,
            List<Replacement> replacements)
    {
        int cursor = from;
        while (cursor < end)
        {
            while (cursor < end && (isSpace(html.charAt(cursor)) || html.charAt(cursor) == ',')) cursor++;
            int start = cursor;
            // Commas inside a URL (in particular data:) are part of the URL token.
            while (cursor < end && !isSpace(html.charAt(cursor))) cursor++;
            int urlEnd = cursor;
            while (urlEnd > start && html.charAt(urlEnd - 1) == ',') urlEnd--;
            imageUrl(html, start, urlEnd, transform, replacements);
            if (urlEnd < cursor) continue;
            int parentheses = 0;
            while (cursor < end)
            {
                char c = html.charAt(cursor++);
                if (c == '(') parentheses++;
                else if (c == ')' && parentheses > 0) parentheses--;
                else if (c == ',' && parentheses == 0) break;
            }
        }
    }

    private static void imageUrl(String html, int from, int end, UnaryOperator<String> transform,
            List<Replacement> replacements)
    {
        while (from < end && isSpace(html.charAt(from))) from++;
        while (end > from && isSpace(html.charAt(end - 1))) end--;
        if (from == end) return;
        String original = html.substring(from, end);
        String converted = transform.apply(original);
        if (!original.equals(converted)) replacements.add(new Replacement(from, end, converted));
    }

    private static String relativeUrl(String url)
    {
        String path = url;
        if (url.regionMatches(true, 0, "https://", 0, 8)
                || url.regionMatches(true, 0, "http://", 0, 7) || url.startsWith("//"))
        {
            int authorityStart = url.startsWith("//") ? 2 : url.indexOf("://") + 3;
            int pathStart = url.indexOf('/', authorityStart);
            int query = url.indexOf('?', authorityStart);
            int fragment = url.indexOf('#', authorityStart);
            if (pathStart <= authorityStart || (query >= 0 && query < pathStart)
                    || (fragment >= 0 && fragment < pathStart)) return url;
            path = url.substring(pathStart);
        }
        else if (!url.startsWith("/"))
        {
            return url;
        }
        String managed = managedPath(path);
        return managed == null ? url : managed;
    }

    private static String managedPath(String path)
    {
        int index = path.indexOf(PROFILE);
        if (index < 0) return null;
        int query = path.indexOf('?');
        int fragment = path.indexOf('#');
        if ((query >= 0 && query < index) || (fragment >= 0 && fragment < index)) return null;
        return path.substring(index);
    }

    private static boolean isSpace(char c)
    {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    private static boolean isLetter(char c)
    {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    private static boolean isName(char c)
    {
        return isLetter(c) || c >= '0' && c <= '9' || c == '-' || c == ':';
    }

    private static String replace(String value, List<Replacement> replacements)
    {
        if (replacements.isEmpty()) return value;
        StringBuilder result = new StringBuilder(value.length());
        int cursor = 0;
        for (Replacement replacement : replacements)
        {
            result.append(value, cursor, replacement.start).append(replacement.value);
            cursor = replacement.end;
        }
        return result.append(value, cursor, value.length()).toString();
    }

    private static final class Replacement
    {
        private final int start;
        private final int end;
        private final String value;

        private Replacement(int start, int end, String value)
        {
            this.start = start;
            this.end = end;
            this.value = value;
        }
    }
}
