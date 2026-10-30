package org.jsoup.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTest {
    private static final String Nl = System.lineSeparator();

    @Test
    void selectsTextFromStandardInput() throws Exception {
        Result result = run("<p>One <b>two</b></p><p>Three</p>", "select", "p", "--text");
        assertEquals(0, result.status);
        assertEquals("One two" + Nl + "Three" + Nl, result.out);
        assertEquals("", result.err);
    }

    @Test
    void selectsAnAttribute() throws Exception {
        Result result = run("<a href='/one'>One</a><a href='/two'>Two</a>", "select", "a", "--attr", "href");
        assertEquals(0, result.status);
        assertEquals("/one" + Nl + "/two" + Nl, result.out);
    }

    @Test
    void cleansWithTheRequestedSafelist() throws Exception {
        Result result = run("<p>Hello <script>alert(1)</script><b>world</b></p>", "clean", "--safelist", "none");
        assertEquals(0, result.status);
        assertEquals("Hello  world" + Nl, result.out);
    }

    @Test
    void rejectsConflictingOutputOptions() throws Exception {
        Result result = run("<p>Text</p>", "select", "p", "--text", "--html");
        assertEquals(2, result.status);
        assertEquals("", result.out);
        assertEquals("jsoup: --html cannot be combined with another output option" + Nl + "Try 'jsoup --help' for usage." + Nl, result.err);
    }

    private static Result run(String input, String... args) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        int status = Main.run(args, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), new PrintStream(output), new PrintStream(error));
        return new Result(status, output.toString("UTF-8"), error.toString("UTF-8"));
    }

    private static class Result {
        final int status;
        final String out;
        final String err;
        Result(int status, String out, String err) {
            this.status = status;
            this.out = out;
            this.err = err;
        }
    }
}

