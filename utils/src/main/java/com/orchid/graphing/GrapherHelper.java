package com.orchid.graphing;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.grapher.GrapherModule;
import com.google.inject.grapher.InjectorGrapher;
import com.google.inject.grapher.graphviz.GraphvizModule;
import com.google.inject.grapher.graphviz.GraphvizRenderer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * User: Igor Petruk
 * Date: 18.02.12
 * Time: 16:22
 */

public class GrapherHelper {
    public static void graph(String filename, Injector injector) throws IOException {
        PrintWriter out = new PrintWriter(new File(filename), "UTF-8");

        Injector injectorWithGrapher = Guice.createInjector(new GrapherModule(), new GraphvizModule());
        GraphvizRenderer renderer = injectorWithGrapher.getInstance(GraphvizRenderer.class);
        renderer.setOut(out).setRankdir("TB");

        injectorWithGrapher.getInstance(InjectorGrapher.class)
                .of(injector)
                .graph();
    }
}
