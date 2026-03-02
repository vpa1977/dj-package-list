package org.debian;

import org.debian.javapackage.dependency.SourceListParser;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

public class AppGetJavaEcosystem {
    public static void main(String[] args) throws IOException {
        SourceListParser sourceListParser = new SourceListParser();
        var deps = sourceListParser.findSourceDependencies(new String[]{
                "default-jdk",
                "default-jdk-headless",
                "default-jre",
                "default-jre-headless",
                "openjdk-8-jdk",
                "openjdk-8-jdk-headless",
        }, "resolute", new String[]{"main", "universe"});
        Collections.sort(deps);
        Iterator it = deps.iterator();
        while (it.hasNext()) {
            System.out.print("./copy-package -y --from ubuntu --from-suite resolute --to ppa:vpa1977/ubuntu/java-25-default-rdep --to-suite resolute ");
            for (int i = 0; i < 20 && it.hasNext(); ++i ) {
                System.out.print(it.next());
                System.out.print(" ");
            }
            System.out.println();
        }
    }
}
