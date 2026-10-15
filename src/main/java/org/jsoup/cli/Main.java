package org.jsoup.cli;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Paths;

/** A small command-line interface for jsoup's parsing, selection, and cleaning APIs. */
public final class Main {
    private Main() {}

    public static void main(String[] args) throws IOException {
        int status = run(args, System.in, System.out, System.err);
        if (status != 0) System.exit(status);
    }

    /** Runs the CLI, returning zero on success and two for invalid usage. */
    public static int run(String[] args, InputStream in, PrintStream out, PrintStream err) throws IOException {
        try {
            if (args.length == 0 || "--help".equals(args[0]) || "help".equals(args[0])) {
                usage(out);
                return 0;
            }
            String command = args[0];
            if ("select".equals(command)) select(args, in, out);
            else if ("get".equals(command)) get(args, out);
            else if ("format".equals(command)) format(args, in, out);
            else if ("clean".equals(command)) clean(args, in, out);
            else throw new IllegalArgumentException("Unknown command: " + command);
            return 0;
        } catch (IllegalArgumentException e) {
            err.println("jsoup: " + e.getMessage());
            err.println("Try 'jsoup --help' for usage.");
            return 2;
        }
    }

    private static void select(String[] args, InputStream in, PrintStream out) throws IOException {
        if (args.length < 2) throw new IllegalArgumentException("select requires a CSS selector");
        Options options = outputOptions(args, 2, true);
        print(load(options.source, in, Parser.htmlParser()).select(args[1]), options, out);
    }

    private static void get(String[] args, PrintStream out) throws IOException {
        if (args.length < 2) throw new IllegalArgumentException("get requires a URL");
        String selector = null;
        Options options = new Options();
        for (int i = 2; i < args.length; i++) {
            if ("--select".equals(args[i])) selector = value(args, ++i, "--select");
            else i = options.parse(args, i, false);
        }
        if (selector == null && (options.text || options.html || options.attribute != null))
            throw new IllegalArgumentException("get output options require --select");
        Document document = Jsoup.connect(args[1]).get();
        if (selector == null) out.println(document.outerHtml());
        else print(document.select(selector), options, out);
    }

    private static void format(String[] args, InputStream in, PrintStream out) throws IOException {
        String source = sourceOption(args, 1, "--xml");
        boolean xml = contains(args, "--xml");
        out.println(load(source, in, xml ? Parser.xmlParser() : Parser.htmlParser()).outerHtml());
    }

    private static void clean(String[] args, InputStream in, PrintStream out) throws IOException {
        String source = null;
        Safelist safelist = Safelist.basic();
        for (int i = 1; i < args.length; i++) {
            if ("--safelist".equals(args[i])) safelist = safelist(value(args, ++i, "--safelist"));
            else if (args[i].startsWith("--")) throw new IllegalArgumentException("Unknown option: " + args[i]);
            else if (source == null) source = args[i];
            else throw new IllegalArgumentException("Only one input may be supplied");
        }
        Document document = load(source, in, Parser.htmlParser());
        out.println(Jsoup.clean(document.body().html(), safelist));
    }

    private static Options outputOptions(String[] args, int start, boolean allowSource) {
        Options options = new Options();
        for (int i = start; i < args.length; i++) i = options.parse(args, i, allowSource);
        return options;
    }

    private static void print(Elements elements, Options options, PrintStream out) {
        for (Element element : elements) {
            if (options.attribute != null) out.println(element.attr(options.attribute));
            else if (options.text) out.println(element.text());
            else if (options.html) out.println(element.html());
            else out.println(element.outerHtml());
        }
    }

    private static Document load(String source, InputStream in, Parser parser) throws IOException {
        if (source == null || "-".equals(source)) return Jsoup.parse(in, null, "", parser);
        if (source.startsWith("http://") || source.startsWith("https://")) {
            Connection connection = Jsoup.connect(source).parser(parser);
            return connection.get();
        }
        return Jsoup.parse(Paths.get(source), null, "", parser);
    }

    private static String sourceOption(String[] args, int start, String permittedOption) {
        String source = null;
        for (int i = start; i < args.length; i++) {
            if (permittedOption.equals(args[i])) continue;
            if (args[i].startsWith("--")) throw new IllegalArgumentException("Unknown option: " + args[i]);
            if (source != null) throw new IllegalArgumentException("Only one input may be supplied");
            source = args[i];
        }
        return source;
    }

    private static boolean contains(String[] args, String value) {
        for (String arg : args) if (value.equals(arg)) return true;
        return false;
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("--"))
            throw new IllegalArgumentException(option + " requires a value");
        return args[index];
    }

    private static Safelist safelist(String name) {
        if ("none".equals(name)) return Safelist.none();
        if ("simpleText".equals(name)) return Safelist.simpleText();
        if ("basic".equals(name)) return Safelist.basic();
        if ("basicWithImages".equals(name)) return Safelist.basicWithImages();
        if ("relaxed".equals(name)) return Safelist.relaxed();
        throw new IllegalArgumentException("Unknown safelist: " + name);
    }

    private static void usage(PrintStream out) {
        out.println("Usage:");
        out.println("  jsoup select <css-selector> [input|-] [--text|--html|--attr <name>]");
        out.println("  jsoup get <url> [--select <css-selector>] [--text|--html|--attr <name>]");
        out.println("  jsoup format [input|-] [--xml]");
        out.println("  jsoup clean [input|-] [--safelist none|simpleText|basic|basicWithImages|relaxed]");
        out.println("Input defaults to standard input. A URL may be used as select or format input.");
    }

    private static final class Options {
        String source;
        String attribute;
        boolean text;
        boolean html;

        int parse(String[] args, int index, boolean allowSource) {
            String arg = args[index];
            if ("--text".equals(arg)) text = output("--text");
            else if ("--html".equals(arg)) html = output("--html");
            else if ("--attr".equals(arg)) {
                output("--attr");
                attribute = value(args, ++index, "--attr");
            } else if (allowSource && !arg.startsWith("--") && source == null) source = arg;
            else if (arg.startsWith("--")) throw new IllegalArgumentException("Unknown option: " + arg);
            else throw new IllegalArgumentException("Only one input may be supplied");
            return index;
        }

        private boolean output(String option) {
            if (text || html || attribute != null)
                throw new IllegalArgumentException(option + " cannot be combined with another output option");
            return true;
        }
    }
}

