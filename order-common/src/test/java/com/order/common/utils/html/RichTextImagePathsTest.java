package com.order.common.utils.html;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RichTextImagePathsTest
{
    private static final String BASE = "https://images.example.test/prod-api";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void convertsMultipleManagedImagesWithoutChangingMarkupOrLinks()
    {
        String html = "<p class='intro'>证书 &amp; 内容\n"
                + "<IMG  SRC = 'https://old.example.test/dev-api/profile/upload/a.png?token=1&amp;size=2#preview' "
                + "width='100' style=\"max-width:100%;\" />\n"
                + "<img src=https://old.example.test/profile/upload/b.jpg alt='B'>"
                + "<a href='https://old.example.test/profile/upload/a.png'>原图</a></p>";
        String expected = "<p class='intro'>证书 &amp; 内容\n"
                + "<IMG  SRC = '/profile/upload/a.png?token=1&amp;size=2#preview' "
                + "width='100' style=\"max-width:100%;\" />\n"
                + "<img src=/profile/upload/b.jpg alt='B'>"
                + "<a href='https://old.example.test/profile/upload/a.png'>原图</a></p>";
        assertEquals(expected, RichTextImagePaths.toRelative(html));
    }

    @Test
    void preservesExternalImagesDataBlobAndNonImageAttributes()
    {
        String html = "<p>https://old.example.test/profile/a.png</p>"
                + "<img src='https://external.example.test/images/a.png'>"
                + "<img src='data:image/png;base64,AAAA'><img src='blob:https://old.example.test/profile/a'>"
                + "<img src='/other/a.png?next=/profile/a.png'>"
                + "<img src='https://external.example.test/a.png#next=/profile/a.png'>"
                + "<div src='https://old.example.test/profile/a.png' "
                + "title=\"<img src='https://old.example.test/profile/a.png'>\"></div>";
        assertEquals(html, RichTextImagePaths.toRelative(html));
    }

    @Test
    void leavesScriptStyleCommentsAndTextOnlyElementsUntouched()
    {
        String fakeImage = "<img src='https://old.example.test/profile/ignored.png'>";
        String prefix = "<!-- " + fakeImage + " -->"
                + "<SCRIPT data-x='>'>const html = \"" + fakeImage + "\";</sCrIpT>"
                + "<style>/* " + fakeImage + " */</style>"
                + "<textarea>" + fakeImage + "</textarea><title>" + fakeImage + "</title>"
                + "<![CDATA[" + fakeImage + "]]>";
        assertEquals(prefix + "<img src='/profile/actual.png'>",
                RichTextImagePaths.toRelative(prefix + "<img src='https://old.example.test/profile/actual.png'>"));
    }

    @Test
    void convertsPictureAndImgSrcsetsWhilePreservingDescriptorsAndDataUrls()
    {
        String html = "<picture><source type='image/webp' srcset=\""
                + "https://old.example.test/dev-api/profile/a.webp 1x, "
                + "https://external.example.test/images/b.webp 2x\">"
                + "<img src='/profile/a.png' srcset='data:image/png;base64,AAAA 1x, "
                + "https://old.example.test/profile/a@2x.png 2x'></picture>"
                + "<source srcset='https://old.example.test/profile/video.png 1x'>";
        String expected = "<picture><source type='image/webp' srcset=\""
                + "/profile/a.webp 1x, https://external.example.test/images/b.webp 2x\">"
                + "<img src='/profile/a.png' srcset='data:image/png;base64,AAAA 1x, "
                + "/profile/a@2x.png 2x'></picture>"
                + "<source srcset='https://old.example.test/profile/video.png 1x'>";
        assertEquals(expected, RichTextImagePaths.toRelative(html));
    }

    @Test
    void supportsProtocolRelativeUrlsProxyPathsAndWhitespaceInSrc()
    {
        assertEquals("<img src='  /profile/a.png?x=1#top  '><img src='/profile/b.png'>",
                RichTextImagePaths.toRelative("<img src='  //old.example.test/dev-api/profile/a.png?x=1#top  '>"
                        + "<img src='/prod-api/profile/b.png'>"));
    }

    @Test
    void makesOnlyRelativeManagedImagesAbsolute()
    {
        String html = "<img src='/profile/a.png?token=1&amp;x=2#top'>"
                + "<img src='https://old.example.test/profile/b.png'><img src='//old.example.test/profile/c.png'>"
                + "<img src='/outside/d.png'><a href='/profile/a.png'>原图</a>";
        assertEquals("<img src='" + BASE + "/profile/a.png?token=1&amp;x=2#top'>"
                        + "<img src='https://old.example.test/profile/b.png'><img src='//old.example.test/profile/c.png'>"
                        + "<img src='/outside/d.png'><a href='/profile/a.png'>原图</a>",
                RichTextImagePaths.toAbsolute(html, " " + BASE + "/// "));
    }

    @Test
    void convertsJsonRichTextAndPreservesOtherTokensExactly() throws Exception
    {
        String escapedHtml = "\"<p>证书<img src=\\\"https:\\/\\/old.example.test\\/dev-api\\/profile\\/a.png?x=1&amp;y=2\\\"></p>\"";
        String prefix = "{\n  \"tip\": \"https:\\/\\/old.example.test\\/profile\\/tip.png\", "
                + "\"name\": \"\\u8bc1\\u4e66\", \"icon\": \"/profile/icon.png\", "
                + "\"count\": 1.00, \"enabled\": true, \"zh\": ";
        String suffix = ", \"nested\": [null, {\"content\": \"<img src='/profile/b.png'>\"}]\n}";
        String source = prefix + escapedHtml + suffix;
        String result = RichTextImagePaths.toRelative(source);
        assertEquals(prefix + JSON.writeValueAsString("<p>证书<img src=\"/profile/a.png?x=1&amp;y=2\"></p>")
                + suffix, result);
        JsonNode original = JSON.readTree(source);
        JsonNode converted = JSON.readTree(result);
        assertEquals(original.get("tip"), converted.get("tip"));
        assertEquals(original.get("name"), converted.get("name"));
        assertEquals(original.get("icon"), converted.get("icon"));
        assertEquals("<img src='" + BASE + "/profile/b.png'>",
                JSON.readTree(RichTextImagePaths.toAbsolute(result, BASE))
                        .path("nested").get(1).path("content").asText());
    }

    @Test
    void supportsTranslatedJsonArraysAndQuotedRootHtml() throws Exception
    {
        String html = "<img src='https://old.example.test/profile/a.png'>";
        String value = "[{\"en\":" + JSON.writeValueAsString(html) + "}, "
                + "{\"zh\":{\"content\":" + JSON.writeValueAsString(html) + "}}]";
        JsonNode converted = JSON.readTree(RichTextImagePaths.toRelative(value));
        assertEquals("<img src='/profile/a.png'>", converted.get(0).path("en").asText());
        assertEquals("<img src='/profile/a.png'>", converted.get(1).path("zh").path("content").asText());
        assertEquals(JSON.writeValueAsString("<img src='/profile/a.png'>"),
                RichTextImagePaths.toRelative(JSON.writeValueAsString(html)));
    }

    @Test
    void keepsInvalidJsonUnchangedRatherThanPartiallyConverting()
    {
        String invalid = "{\"content\":\"<img src='https://old.example.test/profile/a.png'>\", bad}";
        assertEquals(invalid, RichTextImagePaths.toRelative(invalid));
        String multipleRoots = "{\"content\":\"<img src='https://old.example.test/profile/a.png'>\"} {}";
        assertEquals(multipleRoots, RichTextImagePaths.toRelative(multipleRoots));
    }

    @Test
    void preservesEmptyValuesAndMissingBaseUrl()
    {
        assertNull(RichTextImagePaths.toRelative(null));
        assertNull(RichTextImagePaths.toAbsolute(null, BASE));
        assertEquals("", RichTextImagePaths.toRelative(""));
        assertEquals("  ", RichTextImagePaths.toRelative("  "));
        String html = "<img src='/profile/a.png'>";
        assertEquals(html, RichTextImagePaths.toAbsolute(html, null));
        assertEquals(html, RichTextImagePaths.toAbsolute(html, " "));
        assertEquals(html, RichTextImagePaths.toAbsolute(html, "/"));
    }

    @Test
    void textWithLessThanSignsDoesNotHideFollowingImages()
    {
        assertEquals("<p>数值<3，图片<img src='/profile/a.png'></p>",
                RichTextImagePaths.toRelative("<p>数值<3，图片<img src='https://old.example.test/profile/a.png'></p>"));
    }

    @Test
    void conversionsAreIdempotentAndRoundTripManagedImages()
    {
        String source = "<p><img src='https://old.example.test/dev-api/profile/a.png?x=1#top'>"
                + "<img src='https://external.example.test/a.png'></p>";
        String relative = RichTextImagePaths.toRelative(source);
        assertEquals(relative, RichTextImagePaths.toRelative(relative));
        String absolute = RichTextImagePaths.toAbsolute(relative, BASE);
        assertEquals(absolute, RichTextImagePaths.toAbsolute(absolute, BASE));
        assertEquals(relative, RichTextImagePaths.toRelative(absolute));
    }
}
