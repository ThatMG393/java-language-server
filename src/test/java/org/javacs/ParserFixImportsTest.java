package org.javacs;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Collections;
import org.junit.Test;

public class ParserFixImportsTest {
    ExistingImports emptyImports = new ExistingImports(Collections.emptySet(), Collections.emptySet());

    @Test
    public void findJavaUtilList() {
        var resolved = Parser.resolveSymbols(Collections.singleton("List"), emptyImports, Collections.emptySet());
        assertThat(resolved, hasEntry("List", "java.util.List"));
    }

    @Test
    public void findExistingImports() {
        var find = Parser.existingImports(Collections.singleton(JavaCompilerServiceTest.resourcesDir()));
        assertThat(find.classes, hasItem("java.util.List"));
        assertThat(find.packages, hasItem("java.util"));
    }
}
